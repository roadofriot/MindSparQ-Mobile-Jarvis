# 03 — TRD: Technical Requirements Document

## 1. Technology stack

| Concern | Choice | Licence | Why |
|---|---|---|---|
| Language | Kotlin (latest stable 2.x) | Apache-2.0 | Reuses Mobile-Harness code |
| UI | Compose Multiplatform (Android + Desktop JVM) | Apache-2.0 | One UI codebase, native Android |
| Navigation / ViewModel | `org.jetbrains.androidx.navigation:navigation-compose`, `lifecycle-viewmodel-compose` (KMP) | Apache-2.0 | Official KMP ports |
| DI | Koin 4 | Apache-2.0 | KMP, no codegen, light |
| HTTP | Ktor Client 3 (OkHttp engine on Android + JVM), SSE plugin | Apache-2.0 | Streaming, KMP |
| JSON | kotlinx.serialization | Apache-2.0 | |
| Database | SQLDelight 2 (Android driver / JDBC SQLite driver) with **FTS5** | Apache-2.0 | KMP, raw SQL for FTS5 virtual tables |
| MCP | `io.modelcontextprotocol:kotlin-sdk` (client) | MIT | Official SDK, stdio + Streamable HTTP |
| Syntax highlighting | `dev.snipme:highlights` | Apache-2.0 | KMP, no WebView |
| Markdown | `com.mikepenz:multiplatform-markdown-renderer-m3` | Apache-2.0 | |
| Desktop PTY | `org.jetbrains.pty4j:pty4j` | EPL-1.0 | Real terminal on Desktop |
| Desktop secrets | `com.github.javakeyring:java-keyring` (libsecret / Win Credential / macOS Keychain) + AES-GCM file fallback | BSD-3 | OS keychain |
| Android secrets | Android Keystore AES-GCM (ported `ApiKeyVault`) | — | |
| Android background | WorkManager (daily model sync), Foreground Service (agent run) | Apache-2.0 | |
| Desktop packaging | Compose `nativeDistributions` (deb, msi, dmg) | — | |
| Archive | commons-compress + zstd-jni (from MH) | Apache/BSD | runtime bundles, ZIP import |
| Tests | kotlin-test, JUnit4 (Android), Turbine, Ktor MockEngine, Compose UI test (Desktop) | | |

All versions pinned in `gradle/libs.versions.toml`; Renovate-style manual bumps per phase.

## 2. Repository layout

```
MindSparQ-Mobile-Jarvis/
├── AGENTS.md
├── Docs/ (Stitch, specs, evidence)
├── gradle/libs.versions.toml
├── settings.gradle.kts
├── build-logic/                      # convention plugins (kmp-library, compose-feature)
├── apps/
│   ├── android/                      # Android application shell, Manifest, services, JNI (cpp/)
│   └── desktop/                      # Desktop main(), packaging, tray
├── core/
│   ├── model/        (common)        # data classes, enums, ids
│   ├── common/       (common)        # Result, dispatchers, logging (redacting), clock
│   ├── database/     (common + drivers) # SQLDelight schema & DAOs
│   ├── secrets/      (expect/actual) # SecretStore
│   ├── runtime/      (expect/actual) # RuntimeHost: Android=PRoot, Desktop=host
│   ├── policy/       (common)        # PolicyEngine, approvals
│   └── designsystem/ (common)        # Obsidian Kinetic theme & components
├── ai/
│   ├── gateway/      (common/jvm)    # providers, key pool, router, fallback, local HTTP gateway
│   ├── agent/        (common)        # Jarvis Native Agent loop, tools, prompts
│   ├── external/     (android/jvm)   # external CLI agent bridges (ported MH bridges)
│   ├── mcp/          (common/jvm)    # MCP client manager, registry client
│   └── skills/       (common)        # skill registry, installer, loader
├── engine/
│   ├── project/      # templates, import, stack detection, checkpoints
│   ├── git/          # git via runtime CLI (porcelain parsing)
│   ├── index/        # FTS5 indexer, file watcher, embeddings (optional)
│   ├── graph/        # ctags/tree-sitter → symbols/edges
│   ├── memory/       # project memory store
│   ├── loop/         # task state machine, evidence store
│   ├── test/         # test discovery, runners, result parsers
│   ├── build/        # build/run/install pipelines
│   └── voice/        # STT/TTS adapters
└── feature/
    ├── hub/  plan/  studio/  sim/  settings/  projects/  onboarding/
```

Dependency rule: `feature → engine/ai → core`. `engine` and `ai` never import `feature`. Enforced by Gradle module graph + a Konsist test.

## 3. Platform abstraction: `RuntimeHost`

```kotlin
interface RuntimeHost {
    val platform: HostPlatform                     // ANDROID_PROOT, LINUX, WINDOWS, MACOS
    suspend fun health(): RuntimeHealth            // installed?, version, free storage, toolchains
    suspend fun ensureReady(progress: (InstallProgress) -> Unit)
    fun workspaceRoot(projectId: ProjectId): HostPath
    suspend fun exec(spec: ExecSpec): ExecHandle   // non-interactive, streamed
    suspend fun openPty(spec: PtySpec): PtyHandle  // Desktop real PTY; Android line-mode emulation
    suspend fun toolchains(): List<Toolchain>      // node, python, java, gradle, flutter, git, ctags...
    suspend fun installToolchain(id: ToolchainId, progress: (InstallProgress) -> Unit)
}

data class ExecSpec(
    val command: List<String>, val cwd: HostPath, val env: Map<String,String> = emptyMap(),
    val timeout: Duration = 10.minutes, val maxOutputBytes: Long = 4_000_000,
    val origin: ExecOrigin,                        // USER_TERMINAL, AGENT_TOOL, ENGINE
)
interface ExecHandle { val output: Flow<ExecChunk>; suspend fun await(): ExecResult; fun cancel() }
data class ExecResult(val exitCode: Int, val durationMs: Long, val stdoutTail: String,
                      val stderrTail: String, val truncated: Boolean, val cancelled: Boolean)
```

- **Android actual**: wraps ported `RuntimeInstaller`, `NativeSpawnProcess`, `RuntimeExecutionService`. Workspace path inside PRoot `/root/workspaces/<slug>`; host path `filesDir/workspaces/<id>`.
- **Desktop actual**: `ProcessBuilder` / pty4j. Workspaces default `~/MindSparQJarvis/workspaces/<slug>`, or user-chosen existing folder (opened in place). Toolchains = detected on `PATH`; missing ones show install instructions (no silent installs on PC).
- **Every** `exec` passes `PolicyEngine.check(spec)` first (doc 09).

## 4. AI layer (summary — details in doc 06/07)

```kotlin
interface LlmProvider {
    val id: ProviderId; val kind: ProviderKind      // GEMINI, OPENROUTER, OLLAMA, ANTHROPIC, OPENAI_COMPAT
    suspend fun listModels(key: ApiKeyRef?): List<ModelInfo>
    fun chat(req: ChatRequest, key: ApiKeyRef?): Flow<ChatEvent>   // TextDelta, ToolCall, Usage, Done, Error
}
class AiGateway(router: ModelRouter, keyPool: KeyPool, providers: Map<ProviderKind, LlmProvider>) {
    fun chat(task: TaskContext, req: ChatRequest): Flow<GatewayEvent> // includes RouteSwitched events
}
```

`ChatRequest` is a provider-neutral superset: messages (text/image parts), tool schemas (JSON Schema), tool results, `thinking` level, temperature, max tokens. Each provider adapter maps to its native API.

## 5. Jarvis Native Agent

### 5.1 Loop
```
loop (step < maxSteps && !cancelled && budget ok):
    resp = gateway.chat(context.build())          # streamed to UI
    if resp.toolCalls.isEmpty(): finish(resp.text)
    for call in resp.toolCalls:
        decision = policy.evaluate(call, mode)    # ALLOW | ASK | DENY
        if ASK: suspend until user approves/denies (approval card)
        result = tools.execute(call)              # records Evidence
        context.append(toolResult(result.truncatedForModel()))
```

### 5.2 Built-in tools
| Tool | Mode allowed | Default policy |
|---|---|---|
| `list_dir`, `read_file(path, startLine, endLine)`, `search_code(query, kind=fts|regex|symbol)` | Ask/Plan/Work | ALLOW |
| `project_memory_read`, `project_memory_write` | all / Plan+Work | ALLOW |
| `write_file`, `edit_file(path, oldText, newText)`, `delete_file` | Work (Plan: only under `docs/`) | ASK (auto-approve configurable for edits; delete always ASK) |
| `run_command(cmd, cwd, timeout)` | Work | allowlisted → ALLOW, else ASK, denylisted → DENY |
| `git(status|diff|log|add|commit|branch|checkout|push|pull)` | Work (read ops all modes) | push/force/reset → ASK |
| `run_tests(suite?)` | Work | ALLOW |
| `web_fetch(url)` | all | ALLOW (domain denylist) |
| `load_skill(name)` | all | ALLOW |
| `mcp__<server>__<tool>` | per server/tool permission | per config |
| `ask_user(question, options)` | all | — |

### 5.3 Context builder (token budget)
Order: system prompt (rules + mode) → `AGENTS.md`/project rules → enabled skills *index* (name+description only) → project memory summary (≤1.5k tokens) → retrieved snippets (FTS top-k with path:line) → conversation (sliding window + rolling summary) → current turn. Budget = 60 % of model context; summarisation via the cheapest healthy model.

### 5.4 External agents
Ported MH bridges (Claude Code, Antigravity CLI, DeepSeek harness) on Android; Desktop detects `claude`, `agy`, `gemini`, `codex`, `opencode` on PATH. Jarvis exposes its **local gateway** (`http://127.0.0.1:<port>`, Anthropic- and OpenAI-compatible, random port, bearer token per session) so external agents also benefit from key pool + fallback. Antigravity `--dangerously-skip-permissions` becomes a per-project toggle, default **off**.

## 6. Task / loop engine
States: `QUEUED → INSPECTING → PLANNING → AWAITING_APPROVAL → IMPLEMENTING → TESTING → DEBUGGING → VERIFYING → COMPLETED | FAILED | BLOCKED | CANCELLED`.
Limits per task: `maxSteps` (default 60), `maxRepairAttempts` (3), `timeBudget` (30 min), `tokenBudget` (configurable). Checkpoint before first write. Final report generated from Evidence records only. Persisted so the app can be killed and resume at the last completed step.

## 7. Data model (SQLDelight, `core/database`)

```sql
CREATE TABLE project (id TEXT PRIMARY KEY, name TEXT NOT NULL, slug TEXT NOT NULL UNIQUE,
  root_path TEXT NOT NULL, stack TEXT, template TEXT, created_at INTEGER, updated_at INTEGER,
  archived INTEGER DEFAULT 0, deleted_at INTEGER, settings_json TEXT);
CREATE TABLE chat (id TEXT PRIMARY KEY, project_id TEXT REFERENCES project(id) ON DELETE CASCADE,
  title TEXT, agent TEXT NOT NULL, mode TEXT NOT NULL, created_at INTEGER, updated_at INTEGER);
CREATE TABLE message (id TEXT PRIMARY KEY, chat_id TEXT REFERENCES chat(id) ON DELETE CASCADE,
  role TEXT, content_json TEXT, model_ref TEXT, tokens_in INTEGER, tokens_out INTEGER, created_at INTEGER);
CREATE TABLE task (id TEXT PRIMARY KEY, project_id TEXT, chat_id TEXT, title TEXT, state TEXT,
  step INTEGER, repair_attempts INTEGER, budget_json TEXT, started_at INTEGER, ended_at INTEGER, report_md TEXT);
CREATE TABLE evidence (id TEXT PRIMARY KEY, task_id TEXT REFERENCES task(id) ON DELETE CASCADE,
  kind TEXT,  -- COMMAND, DIFF, TEST_RUN, BUILD, FILE_WRITE, MCP_CALL, ROUTE_SWITCH, APPROVAL
  summary TEXT, exit_code INTEGER, payload_path TEXT, created_at INTEGER);
CREATE TABLE checkpoint (id TEXT PRIMARY KEY, project_id TEXT, task_id TEXT, label TEXT, git_ref TEXT, created_at INTEGER);
CREATE TABLE provider (id TEXT PRIMARY KEY, kind TEXT, display_name TEXT, base_url TEXT, enabled INTEGER, config_json TEXT);
CREATE TABLE api_key (id TEXT PRIMARY KEY, provider_id TEXT REFERENCES provider(id) ON DELETE CASCADE,
  label TEXT, secret_ref TEXT NOT NULL, state TEXT, cooldown_until INTEGER, last_error TEXT,
  priority INTEGER, requests_today INTEGER, tokens_today INTEGER, last_used_at INTEGER);
CREATE TABLE model (provider_id TEXT, model_id TEXT, display_name TEXT, context_length INTEGER,
  supports_tools INTEGER, supports_vision INTEGER, supports_thinking INTEGER, is_free INTEGER,
  open_weights INTEGER, created_at INTEGER, last_seen_at INTEGER, pinned INTEGER DEFAULT 0, hidden INTEGER DEFAULT 0,
  PRIMARY KEY(provider_id, model_id));
CREATE TABLE route_profile (id TEXT PRIMARY KEY, name TEXT, task_class TEXT, chain_json TEXT, is_default INTEGER);
CREATE TABLE usage_daily (day TEXT, provider_id TEXT, model_id TEXT, key_id TEXT,
  requests INTEGER, tokens_in INTEGER, tokens_out INTEGER, cost_usd REAL, PRIMARY KEY(day, provider_id, model_id, key_id));
CREATE TABLE mcp_server (id TEXT PRIMARY KEY, name TEXT, transport TEXT, command_json TEXT, url TEXT,
  env_refs_json TEXT, scope TEXT, project_id TEXT, enabled INTEGER, source TEXT, created_at INTEGER);
CREATE TABLE mcp_tool_permission (server_id TEXT, tool_name TEXT, policy TEXT, PRIMARY KEY(server_id, tool_name));
CREATE TABLE skill (id TEXT PRIMARY KEY, name TEXT, description TEXT, source_url TEXT, version TEXT,
  path TEXT, scope TEXT, has_scripts INTEGER, reviewed INTEGER, installed_at INTEGER);
CREATE TABLE skill_enablement (skill_id TEXT, project_id TEXT, enabled INTEGER, PRIMARY KEY(skill_id, project_id));
CREATE TABLE memory_item (id TEXT PRIMARY KEY, project_id TEXT, kind TEXT, -- FACT, DECISION, TASK_STATE, KNOWN_ISSUE, PREFERENCE
  content TEXT, source TEXT, pinned INTEGER, created_at INTEGER, updated_at INTEGER);
CREATE TABLE indexed_file (project_id TEXT, path TEXT, sha1 TEXT, size INTEGER, lang TEXT, indexed_at INTEGER, PRIMARY KEY(project_id, path));
CREATE VIRTUAL TABLE chunk_fts USING fts5(project_id UNINDEXED, path UNINDEXED, start_line UNINDEXED,
  end_line UNINDEXED, symbol, content, tokenize = 'unicode61 remove_diacritics 2');
CREATE TABLE chunk_vec (project_id TEXT, path TEXT, start_line INTEGER, model TEXT, dim INTEGER, vec BLOB);
CREATE TABLE symbol (id INTEGER PRIMARY KEY, project_id TEXT, name TEXT, kind TEXT, path TEXT, line INTEGER, scope TEXT);
CREATE TABLE edge (project_id TEXT, src_symbol INTEGER, dst_name TEXT, kind TEXT); -- IMPORTS, CALLS, REFERENCES, TESTS
CREATE TABLE policy_rule (id TEXT PRIMARY KEY, scope TEXT, project_id TEXT, kind TEXT, pattern TEXT, action TEXT);
CREATE TABLE audit_event (id TEXT PRIMARY KEY, ts INTEGER, actor TEXT, action TEXT, target TEXT, outcome TEXT, detail_json TEXT);
```
Large payloads (full logs, diffs) live in `appData/evidence/<task>/<id>.txt`, never in rows. Schema migrations via SQLDelight `.sqm` files, each with a migration test.

## 8. Concurrency & resources
- One active agent task per project; global max 2 (Android) / 4 (Desktop).
- Max 2 concurrent `exec` processes on Android, 4 on Desktop (configurable).
- Indexer runs on `Dispatchers.IO.limitedParallelism(1)` and pauses while a build runs.
- Android: agent task runs inside the foreground service with an ongoing notification (Stop action).

## 9. Logging & observability
Structured local logs (`appData/logs`, rotated 5×2 MB) through a `Redactor` that masks key patterns (`AIza…`, `sk-…`, `sk-or-…`, `sk-ant-…`, Bearer headers, `ghp_…`). Diagnostics export = logs + DB schema version + device info, never secrets. No remote telemetry.

## 10. Build & CI
- `./gradlew :apps:desktop:run`, `:apps:android:assembleDebug`, `check` (unit + Konsist + detekt).
- GitHub Actions: `check` on ubuntu; Android APK + Linux deb artifacts on tag; Windows/macOS packages on tag via matrix.
- Native JNI (PRoot launcher) built with NDK 28 as in MH.

