# 05 — Screen & Button Specification

Every interactive control has: **ID · Label/Icon · Action · Logic (what code runs) · States · Errors**. No control may ship without its row here. Shortcut column applies to Desktop.

Conventions: `VM` = screen ViewModel; `→` navigates; `⟶ approval` = goes through `PolicyEngine` and may raise an approval card.

---

## 0. Global chrome

### 0.1 Top bar (all screens once a project is open)
| ID | Control | Action | Logic | States / errors | Shortcut |
|---|---|---|---|---|---|
| G-01 | Project switcher (name ▾) | Open project picker sheet | `ProjectRepository.observeRecent()`; selecting sets `ActiveProject` in `SessionStore` and cancels nothing (tasks are per-project) | Shows dot if a task runs in another project | Ctrl+P |
| G-02 | Branch pill (`main`) | Open Git sheet (0.3) | `GitService.status()` | Indigo; `*` suffix if dirty; red if conflict | Ctrl+Shift+G |
| G-03 | Model chip | Open model picker sheet (0.4) | Shows active route head model + health dot from `KeyPool.health(provider)` | green healthy / amber some keys cooling / red none usable | Ctrl+M |
| G-04 | Run ▶ | Run project default target | `RunService.runDefault(project)` (dev server or app); streams to bottom dock | becomes ■ Stop while running | F5 / Shift+F5 |
| G-05 | Task indicator (spinner + count) | Open Task drawer | lists active/queued tasks with state; tap → Hub timeline | hidden when none | Ctrl+J |
| G-06 | Notifications bell | Open notifications | approvals waiting, task finished/failed, model sync result | badge count = unread | — |
| G-07 | Voice (graphic_eq) | Start dictation into the focused composer | `VoiceService.dictate(locale)` | pulsing while listening; error "Microphone permission denied" with Settings link | Ctrl+Shift+Space |

### 0.2 Bottom navigation (Compact) / Nav rail (Medium/Expanded)
| ID | Item | Destination | Badge |
|---|---|---|---|
| N-01 | Hub (neurology) | Hub | pending approvals |
| N-02 | Plan (description) | Plan | stale specs |
| N-03 | Studio (code_blocks) | Studio | unsaved files |
| N-04 | Sim (devices) | Sim | running server dot |
| N-05 | Settings (settings) | Settings | provider error dot |

### 0.3 Git sheet
| ID | Control | Logic | Errors |
|---|---|---|---|
| GIT-01 | File list with checkboxes | `git status --porcelain=v2` parsed | — |
| GIT-02 | Tap file | → Studio diff for file | — |
| GIT-03 | Stage / Unstage | `git add` / `git restore --staged` | — |
| GIT-04 | Commit message field + ✦ "Generate" | `AiGateway` cheap route summarises staged diff (≤ 8k tokens, truncated) | no model → button disabled with tooltip |
| GIT-05 | Commit | `git commit -m` ; evidence recorded | empty message → inline error; hook failure → show output |
| GIT-06 | Branch ▾ (switch/create) | `git switch [-c]` ; blocks if dirty unless "stash & switch" chosen | conflict → error sheet |
| GIT-07 | Pull | `git pull --ff-only` | non-ff → offer "rebase" or "merge" choice |
| GIT-08 | Push | ⟶ approval (always on first push per remote); `git push` with token from SecretStore via `GIT_ASKPASS` helper | auth failure → link to GitHub account settings |
| GIT-09 | Force push (overflow) | ⟶ approval HIGH; `--force-with-lease` only | — |
| GIT-10 | Log | `git log --oneline -n 100`; tap → commit diff | — |

### 0.4 Model picker sheet
| ID | Control | Logic |
|---|---|---|
| MP-01 | Route profile tabs (Auto / Fast / Smart / Free / Local) | sets chat's `routeProfileId` |
| MP-02 | Model list grouped by provider (search box) | from `model` table where not hidden; shows ctx, tools ✓, vision ✓, free badge, open-weights badge |
| MP-03 | Tap model | pins model as head of chain for this chat only (rest of profile chain stays as fallback) |
| MP-04 | "Refresh models" | `ModelCatalog.refresh(all)`; shows per-provider result |
| MP-05 | Thinking level (Off/Low/Med/High) | shown only if model `supports_thinking` |

---

## 1. Onboarding (first launch)
| ID | Step / Control | Logic | Errors |
|---|---|---|---|
| ON-01 | Welcome → "Get started" | — | — |
| ON-02 | Language: English / नेपाली | writes `AppSettings.locale`; recomposes | — |
| ON-03 | Theme: Dark / Light / System | `AppSettings.theme` | — |
| ON-04 | (Android) Runtime setup "Install runtime (~1.2 GB)" | `RuntimeHost.ensureReady` with progress; foreground service; resumable | low storage → blocked with required/free numbers; network error → Retry |
| ON-04b | (Desktop) Toolchain check | lists git/node/python/java/flutter found on PATH with versions | missing git → blocking: install instructions per OS |
| ON-05 | Add first provider: Gemini key / OpenRouter key / Ollama URL / Skip | opens Provider editor (8.2) inline; "Test" must pass before Next unless Skip | — |
| ON-06 | Finish → Projects | — | — |

## 2. Projects (start destination)
| ID | Control | Action / Logic | States / errors |
|---|---|---|---|
| PR-01 | Search field | filters by name/stack (local) | — |
| PR-02 | Project card tap | open → Hub of project; sets ActiveProject | missing folder (Desktop) → "Locate folder" / "Remove" |
| PR-03 | Card overflow: Rename | dialog; updates `project.name` (slug unchanged) | duplicate allowed (slug unique) |
| PR-04 | Card overflow: Archive / Unarchive | `archived=1`; hidden from default list | — |
| PR-05 | Card overflow: Export ZIP | `ProjectExporter.zip(project, excludeBuildDirs=true)` → SAF save (Android) / file dialog (Desktop) | progress; failure shows reason |
| PR-06 | Card overflow: Delete | confirm dialog typing project name; moves workspace to `trash/` + `deleted_at`; purge after 7 days by worker | running task → must stop first |
| PR-07 | FAB "+ New" | opens New Project sheet | — |
| NP-01 | Tab "Template" → grid (Web/React-Vite, Python, Node/TS, Flutter, Android Kotlin, C/C++, Empty) + Name field + "Create" | `TemplateService.create(template, name)` runs real scaffolding (e.g. `npm create vite@latest -- --template react-ts`, `flutter create`, MH Android generator) inside runtime; then `git init` + first commit; opens Hub | toolchain missing → offer "Install toolchain" (Android) / instructions (Desktop) |
| NP-02 | Tab "Describe" → text area + "Plan it" | creates Empty project, opens Plan with prompt prefilled and runs `/plan` in Plan mode | — |
| NP-03 | Tab "Clone" → URL, branch (opt), "Use GitHub account" toggle, "Clone" | `git clone --depth 50` (full on request) with credential helper | auth error, repo too large warning (> 500 MB) |
| NP-04 | Tab "Import" → ZIP picker / Folder picker (Desktop: open in place toggle) | `SafeZipExtractor` (zip-slip check, symlink reject, size & count limits 2 GB / 100k files) | rejected entries listed |
| PR-08 | Trash (overflow) | list deleted projects; Restore / Delete forever | — |

## 3. Hub (AI workspace)

### 3.1 Header row
| ID | Control | Logic |
|---|---|---|
| HB-01 | Chat selector ▾ (list, "+ New chat") | `chat` table per project; new chat inherits agent & mode |
| HB-02 | Agent selector: Jarvis (default) / Claude Code / Antigravity / Gemini CLI / Codex / OpenCode / DeepSeek | only installed/detected agents enabled; others show "Install" (Android runtime) or "Not found on PATH" |
| HB-03 | Mode segmented: Ask · Plan · Work | sets `chat.mode`; switching to Work while a plan exists shows "Execute approved plan?" |

### 3.2 Timeline
| ID | Element | Logic |
|---|---|---|
| HB-10 | User / assistant messages (markdown, code blocks with Copy, "Apply to file" for code blocks with a path) | "Apply" creates an `edit_file` proposal ⟶ approval |
| HB-11 | Tool event rows (read_file, run_command, …) | collapsed one-liners; expand shows args + output tail + exit code; "Open full log" opens evidence file |
| HB-12 | Route switch row (amber) "Gemini key ‘main’ → 429, switched to ‘backup’" | from `GatewayEvent.RouteSwitched` |
| HB-13 | Approval card (see DRD) — Approve / Edit / Deny / Always allow | resumes suspended tool call; "Always allow" writes `policy_rule` (project scope) |
| HB-14 | Task report card (on completion) | rendered from evidence: files changed, commands, tests (PASS/FAIL/NOT RUN), limitations; buttons "View diff", "Revert to checkpoint", "Commit" |
| HB-15 | Message overflow: Copy, Retry (same route), Retry with model…, Edit & resend, Delete | retry re-issues from that message; later messages archived |

### 3.3 Composer
| ID | Control | Action / Logic | Errors |
|---|---|---|---|
| HB-20 | Text field (✦ prefix) | multiline; `/` opens palette; `@` opens file/symbol mention picker (inserts path refs) | — |
| HB-21 | Attach (paperclip) | pick files → copied to `.jarvis/attachments/` of project; images sent as image parts if model supports vision, else warns | size > 20 MB rejected |
| HB-22 | Camera (Android) | capture photo → attachment | permission denied → message |
| HB-23 | Mic | dictation into field (does not auto-send) | — |
| HB-24 | Send ▶ | `TaskEngine.submit(chat, text, attachments)`; button becomes ■ Stop | no usable route → error card with "Open Providers" |
| HB-25 | Stop ■ | cancels gateway stream + running exec (SIGTERM then SIGKILL after 5 s); task → CANCELLED with partial report | — |
| HB-26 | Context meter (mono `12.4k / 1M`) | real token estimate of next request; tap shows breakdown (system, skills, memory, retrieval, history) | — |

### 3.4 Presets row & palette
| ID | Control | Logic |
|---|---|---|
| HB-30 | Preset chips (default: Audit project, Write tests, Fix failing tests, Explain codebase, Security review, Optimize performance) | inserts template text; long-press → edit/delete; "VIEW ALL" → preset manager |
| HB-31 | Slash palette items: `/plan`, `/audit`, `/test`, `/fix`, `/review`, `/commit`, `/skill <name>`, `/mcp`, `/model <id>`, `/memory`, `/clear` | each maps to a typed command handler in `CommandRouter` |

### 3.5 Agents panel ("Active tasks")
| ID | Control | Logic |
|---|---|---|
| HB-40 | Task row: title, state pill, current step text (real tool name + target), elapsed | from `task` + live events |
| HB-41 | Row tap → scroll timeline to task; overflow: Stop, Pause after step, Open report | — |

## 4. Plan (Spec Studio)
| ID | Control | Action / Logic | States |
|---|---|---|---|
| PL-01 | Spec tree (`docs/` of project): PRD.md, TRD.md, DRD.md, ARCHITECTURE.md, IMPLEMENTATION_PLAN.md, TEST_PLAN.md, ACCEPTANCE_CRITERIA.md, AUDIT_REPORT.md, ADR/ | file list + status: Generated / Edited (git dirty) / Stale (older than last code change touching referenced paths) | — |
| PL-02 | Tap spec | opens markdown preview; toggle Edit (editor) | — |
| PL-03 | "+ new_spec.md" | dialog name + type template → creates file | exists → error |
| PL-04 | Diff Preview (per spec) | `git diff -- docs/<file>` | none → disabled |
| PL-05 | Re-parse spec tree (refresh) | rescans folder; recomputes stale status | — |
| PL-06 | Model chips row (Smart / Fast / specific) | sets route for Plan generation only | — |
| PL-07 | Constraint chips (user-defined, e.g. "Offline-first", "Supabase", "Flutter") + "+ add" | stored in project memory as DECISION/PREFERENCE; included in spec prompts | — |
| PL-08 | Decision input "Dictate decision or specify constraint…" + mic + send | appends ADR entry (`docs/adr/NNNN-title.md`) and memory item | — |
| PL-09 | **Synthesize Complete Project Specs** | starts Plan-mode task running skill `requirements-and-planning`: audit (if code exists) → PRD → TRD → DRD → Architecture → Implementation plan → Test plan; each written via `write_file` under `docs/` (allowed in Plan mode) | running → shows per-doc progress; failures per doc |
| PL-10 | Task board tab | parses `IMPLEMENTATION_PLAN.md` task list (`- [ ] T-xx …` with `deps:`) into DAG; tap task → "Run in Work mode" | invalid format → shows parse errors with line |
| PL-11 | History (clock) | spec commit history `git log -- docs/` | — |
| PL-12 | Commit Spec | stages `docs/` and opens Git sheet with generated message | — |
| PL-13 | Clean Workspace | `git status` of untracked generated files under `.jarvis/tmp` → confirm delete | — |

## 5. Studio (Code)

Tabs: **Files · Editor · Diff · Terminal · Tests · Graph** (Compact: top tabs; Expanded: tree + editor + bottom dock).

### 5.1 Files
| ID | Control | Logic |
|---|---|---|
| ST-01 | Tree (lazy, respects `.gitignore` toggle) | `RuntimeHost` file listing; git status colouring |
| ST-02 | Long-press/right-click: New file, New folder, Rename, Delete (⟶ confirm), Copy path, Reveal in terminal, Ask Jarvis about this | each a file op through `WorkspaceFs` (path-jailed) |
| ST-03 | Search (magnifier): modes Text (FTS), Regex (ripgrep in runtime), Symbol (graph) | results with path:line, tap opens editor at line |

### 5.2 Editor
| ID | Control | Logic |
|---|---|---|
| ST-10 | Text area with highlighting, line numbers, soft wrap toggle | `highlights` lib; files > 2 MB open read-only |
| ST-11 | Save (Ctrl+S) / autosave toggle | write via `WorkspaceFs`; triggers index refresh |
| ST-12 | Undo/redo | per-buffer history |
| ST-13 | Find/replace | in-buffer |
| ST-14 | ✦ Ask about selection | opens Hub composer with `@file:L10-L40` reference |
| ST-15 | Extra-keys bar (Android): Tab, Esc, Ctrl, arrows, `{}`, `()`, `;` | inserts characters/modifiers |

### 5.3 Diff
| ID | Control | Logic |
|---|---|---|
| ST-20 | Source selector: Working tree / Staged / Task #id / Commit | git diff variants or task's checkpoint→HEAD |
| ST-21 | Hunk Accept / Reject | reject = reverse-apply hunk (`git apply -R` on hunk patch) |
| ST-22 | Revert all to checkpoint | ⟶ confirm; `WorkspaceCheckpoints.restore` |
| ST-23 | Accept & Commit | stage accepted hunks → Git sheet |

### 5.4 Terminal
| ID | Control | Logic |
|---|---|---|
| ST-30 | Session tabs + "+" | `RuntimeHost.openPty` (Desktop PTY / Android line-mode) |
| ST-31 | Input line (`>` prefix), history ↑↓ | user commands go through policy with origin USER_TERMINAL (only hard-deny list applies) |
| ST-32 | Ctrl-C button, Clear, Copy all, Scroll-lock, Share log | — |
| ST-33 | "Explain error" (appears when last exit ≠ 0) | sends last 200 lines to Hub in Ask mode |

### 5.5 Tests
| ID | Control | Logic |
|---|---|---|
| ST-40 | Discovered suites list | `TestDiscovery` per stack (pytest, vitest/jest, flutter test, gradle test, ctest, go test) |
| ST-41 | Run all / Run suite / Run failed | `TestRunner` with JUnit-XML / JSON reporters where available; parsed into PASS/FAIL/SKIP counts |
| ST-42 | Failure row → stack trace, "Open at line", ✦ "Fix with Jarvis" | starts Work-mode `/fix` task with failure evidence attached |
| ST-43 | Coverage (if reporter available) | shows % only when produced; otherwise hidden |

### 5.6 Graph (Architecture)
| ID | Control | Logic |
|---|---|---|
| ST-50 | Node graph (modules → files → symbols, zoom levels) | from `symbol`/`edge` tables; force layout limited to 300 nodes, else module level |
| ST-51 | Node tap → details (path, symbols, dependents, related tests) | queries edges |
| ST-52 | "Impact of change" (select node) | reverse-dependency BFS depth 3 → list of files & tests |
| ST-53 | Rebuild graph | `GraphIndexer.rebuild(project)` |
| ST-54 | Instruct input "Instruct Jarvis to tweak architecture…" | Hub task in Plan mode with selected nodes as context |
| ST-55 | Export (share) | Mermaid file `docs/architecture-graph.md` |

## 6. Sim (Run & Ship)

### 6.1 Preview
| ID | Control | Logic | Errors |
|---|---|---|---|
| SM-01 | Target selector: Web dev server / Android APK / Desktop app (Desktop host only) / Custom command | from stack detection + `.jarvis/run.json` | — |
| SM-02 | Start / Stop server | `RunService` exec long-running; detects port from output regex or config; port conflict → picks next & tells | crash → logs + "Explain error" |
| SM-03 | Viewport presets: Phone 390×844, Tablet 820×1180, Desktop 1440×900, Fit | WebView scales | — |
| SM-04 | Rotate | swaps width/height | — |
| SM-05 | Reload | WebView reload (hot reload is framework-native via dev server) | — |
| SM-06 | Inspector toggle (web) | injects JS overlay; tap element → shows tag, classes, computed padding/margins/colours; "Ask Jarvis to change this" prefilled with selector | — |
| SM-07 | Console tab | `WebChromeClient.onConsoleMessage` (Android) / JCEF console (Desktop) captured | — |
| SM-08 | Open in external browser | system browser to `http://127.0.0.1:<port>` | — |

Desktop preview uses **JCEF** (via `compose-webview-multiplatform` / KCEF) loaded lazily on first use (≈ 150 MB download, user-confirmed) — or "Open in browser" only if declined.

### 6.2 Android build & install
| ID | Control | Logic | Errors |
|---|---|---|---|
| SM-20 | Build APK (Debug/Release) | `./gradlew assembleDebug` (or flutter build apk) in runtime with low-mem flags; evidence | toolchain missing → install pack |
| SM-21 | Artifact card: name, size, SHA-256, versionCode/name (aapt2 dump) | computed after build | — |
| SM-22 | Install | Android: `PackageInstaller` session (ported `AndroidAppInstaller`) → system confirmation; result broadcast recorded. Desktop: `adb install -r` to selected device | user cancelled → status CANCELLED |
| SM-23 | Launch | Android: `packageManager.getLaunchIntentForPackage`; Desktop: `adb shell monkey -p pkg 1` | not installed → disabled |
| SM-24 | Device selector (Desktop) | `adb devices -l` | adb missing → instructions |
| SM-25 | Logcat (Desktop only) | `adb logcat --pid` filtered | — |

### 6.3 Pipeline
| ID | Control | Logic |
|---|---|---|
| SM-40 | Stages: Verify (lint + tests) → Build → Package → Deliver | each stage = configured commands in `.jarvis/pipeline.json` (auto-generated per stack, editable) |
| SM-41 | Target: Debug / Release / GitHub Release | Release requires signing config (keystore picked by user, password in SecretStore) |
| SM-42 | Run pipeline | sequential; stops at first failure; durations measured |
| SM-43 | Log stream with filter (stage, level), copy, share | — |
| SM-44 | Deliver to GitHub Release | ⟶ approval; GitHub REST upload with user token | — |

## 7. Settings

### 7.1 Sections
Profile · Providers & Keys · Routing · Models · MCP Servers · Skills · Agents · Policy & Approvals · Runtime & Toolchains · Intelligence (index/graph/memory) · Voice · Appearance & Language · Storage · Usage · About & Licences · Diagnostics.

| ID | Control | Logic |
|---|---|---|
| SE-01 | Profile: name, avatar, GitHub account (OAuth device flow) | token in SecretStore; used for clone/push/MCP GitHub |
| SE-02 | Preferences: response verbosity (Terse / Detailed), default mode, auto-approve level (None / Edits / Edits+safe commands) | `AppSettings` |
| SE-03 | Storage: workspaces size, runtime size, caches; "Clear build caches" (per project, confirm), "Clear index" | computed via `du` in runtime |
| SE-04 | Export all (projects metadata + settings, no secrets) | JSON + ZIP |
| SE-05 | Usage: per day/provider/model tokens & estimated cost (only when price known) | `usage_daily` |
| SE-06 | Diagnostics: view logs, export redacted bundle | — |
| SE-07 | About & Licences | renders `assets/licenses/*` incl. PRoot GPL notice and MH MIT notice |

Providers, Routing, Models → doc 06 §6. MCP, Skills → doc 07 §5. Policy → doc 09 §4. Runtime → below.

### 7.2 Runtime & Toolchains (Android)
| ID | Control | Logic |
|---|---|---|
| RT-01 | Status card (installed version, health, free space) | `RuntimeHost.health()` |
| RT-02 | Repair runtime | ported `RootfsRepair` |
| RT-03 | Toolchain packs: Python, Node, Java+Android SDK, Flutter, C/C++, Ctags/ripgrep — Install / Remove / size | `installToolchain`; resumable downloads; SHA-256 verify |
| RT-04 | Reset runtime (danger) | confirm by typing RESET; workspaces preserved |

