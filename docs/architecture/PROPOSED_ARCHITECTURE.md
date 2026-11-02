# MindSparQ Mobile Jarvis — Proposed Enterprise Architecture

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Architecture Pattern:** Modular Clean Architecture with Sandbox Virtualization & Directed Acyclic Graph (DAG) Loop

---

## 1. System Topology

```
┌────────────────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER (UI)                         │
│  PocketWorkspaceScreen · PocketChatTab · TerminalScreen · ChangesView │
│  Jetpack Compose Multiplatform · Design Tokens · Accessible Components│
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                     DELEGATE ORCHESTRATION LAYER                       │
│  MainViewModel · ChatDelegate · TerminalDelegate · ProjectGitDelegate  │
│  StateFlow<AppUiState> · Coroutines · Permission Gate Prompts          │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                  AUTONOMOUS ENGINEERING ENGINE (:engine)               │
│  TaskOrchestrator (State Graph) · ChangeImpactAnalyzer · TestParser    │
│  FtsIndexManager · CodeChunker · ProjectMemoryStore · VoiceService     │
└───────────────────────┬──────────────────────────┬─────────────────────┘
                        │                          │
┌───────────────────────▼────────┐        ┌────────▼─────────────────────┐
│    AI & GATEWAY LAYER (:ai)    │        │  POLICY & GOVERNANCE (:core) │
│  KeyPoolManager · ModelCatalog │        │  PolicyEngine · Jail Check   │
│  LocalFormatGateway (Port 14455│        │  SecureSecretStore (Keystore)│
│  OpenCode CLI (dsh) Integration│        │  JarvisDatabase (SQLDelight) │
└───────────────────────┬────────┘        └────────┬─────────────────────┘
                        │                          │
┌───────────────────────▼──────────────────────────▼─────────────────────┐
│                    PRoot LINUX GUEST USERLAND                         │
│  /workspace/<slug> (Jail) · /usr/local/bin/dsh · git · python3 · node  │
│  Scoped Stdio MCP Servers (Filesystem · Context7 · Playwright)         │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Module Boundaries & Responsibilities

| Module | Purpose | Allowed Dependencies | Forbidden Dependencies |
| :--- | :--- | :--- | :--- |
| `:core:model` | Core domain entities (`Project`, `Chat`, `Message`, `ExecutionMode`, `TaskState`). | None (pure Kotlin / Serialization) | `:app`, `:core:database`, `:ai:*` |
| `:core:database` | SQLDelight schema, ACID persistence, relational queries, B-Tree indexes. | `:core:model` | `:app`, `:ai:*`, `:engine:*` |
| `:core:policy` | `PolicyEngine`, command screening, jail boundary check, `SecureSecretStore`. | `:core:model` | `:app`, `:ai:*` |
| `:core:designsystem` | Tokens, typography, accessible components, responsive layouts. | `:core:model` | `:app`, `:ai:*`, `:engine:*` |
| `:core:runtime` | Memory governor, process execution wrappers, wake locks. | `:core:model` | `:app` |
| `:ai:gateway` | Model catalogs, routing, key pools, multi-provider failover, `LocalFormatGateway`. | `:core:model`, `:core:policy` | `:app` |
| `:ai:mcp` | MCP registry, configuration exporter, tool permission mapping. | `:core:model`, `:core:policy` | `:app` |
| `:ai:skills` | SkillManager, `SKILL.md` parser, provenance and license verification. | `:core:model`, `:core:policy` | `:app` |
| `:engine:loop` | `TaskOrchestrator` finite state machine, resumable execution loops. | `:core:model`, `:core:policy`, `:ai:gateway` | `:app` |
| `:engine:index` | FTS5 full-text indexing, code AST chunking, symbol retrieval. | `:core:model`, `:core:database` | `:app` |
| `:engine:memory`| Project memory store, persistent architectural decisions. | `:core:model`, `:core:database` | `:app` |
| `:engine:test` | Test result parsing (Gradle, pytest, vitest, flutter test). | `:core:model` | `:app` |
| `:engine:voice`| Conversational voice service, barge-in detection, speech-to-speech. | `:core:model` | `:app` |
| `:app` | Android application entry point, PRoot virtualization, Compose screens. | All `:core:*`, `:ai:*`, `:engine:*` | None |

---

## 3. Finite State Machine: `TaskOrchestrator`

The engineering loop operates through 10 explicit states:

```text
[INTAKE] ──► [DISCOVERY] ──► [REQUIREMENTS] ──► [ARCHITECTURE] ──► [AWAITING_APPROVAL]
                                                                          │
                                                                   (User Approved)
                                                                          │
                                                                          ▼
[COMPLETED] ◄── [VERIFYING] ◄── [TESTING] ◄── [IMPLEMENTING] ◄────────────┘
     ▲              │                │
     │       (Tests Failed)   (Compiler Error)
     │              │                │
     └───────── [DEBUGGING] ◄────────┘
```

Each state transition requires **deterministic evidence**:
- From `ARCHITECTURE` to `AWAITING_APPROVAL`: Generation of PRD and technical plan.
- From `IMPLEMENTING` to `TESTING`: Diff inspection and clean compilation exit code 0.
- From `TESTING` to `VERIFYING`: Executed test suite outputs (100% pass).
- From `VERIFYING` to `COMPLETED`: Git status clean, no regressions, and user acceptance.
