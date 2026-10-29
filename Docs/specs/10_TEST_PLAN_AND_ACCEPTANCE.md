# 10 — Test Plan & Acceptance Scenarios

> This document defines the testing strategy, automated test commands, and formal acceptance criteria for MindSparQ Jarvis across Android and PC Desktop.

---

## 1. Testing Strategy & Layers

| Layer | Framework / Tool | Execution Environment | Target |
|---|---|---|---|
| **Unit Tests** | JUnit4 / kotlin-test | Local JVM (Laptop / CI) | Data models, AI Gateway, Router, Parsers, Loop State Machine |
| **Architecture Tests** | Konsist | Local JVM | Enforces module boundaries (`feature` never imported by `core/ai/engine`) |
| **Integration Tests** | Ktor `MockEngine`, Turbine | Local JVM | Streaming chat responses, error recovery, fallback chains, MCP transport |
| **Runtime Integration** | Bash / Native Exec tests | Android PRoot / Host Shell | Process spawning, file jailing, ctags indexing, git commands |
| **UI Component Tests** | Compose UI Test (Desktop) | Local JVM | Theme token contrast, dialog flows, timeline rendering |

### Automated Commands
```bash
# 1. Run all common & desktop unit tests with low-memory ceiling
./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m" check

# 2. Run architecture guardrails
./gradlew :core:common:test --tests "com.mindsparq.jarvis.architecture.*"

# 3. Assemble Android online debug APK
./gradlew :apps:android:assembleOnlineDebug
```

---

## 2. Formal Acceptance Scenarios (A through L)

Every major release or completed phase requires verifiable evidence matching these 12 scenarios:

### Scenario A — Project Creation & Persistence
- **Action**: Create a new project using the "Vite + React (TypeScript)" template.
- **Verification**: Files scaffold correctly, Git initializes with an initial commit, the workspace re-opens reliably after closing and restarting the application.

### Scenario B — Repository Audit & Discovery
- **Action**: Clone or import a GitHub repository.
- **Verification**: Technology stack is detected automatically; `AUDIT_REPORT.md` is generated under `docs/` with evidence-backed findings and no fabricated metrics.

### Scenario C — Controlled AI Implementation
- **Action**: Submit a prompt: "Add dark-mode toggle to settings".
- **Verification**: Agent runs in Plan mode first, produces an implementation diff, waits for user approval, applies the change in Work mode, runs tests, and outputs a completion report.

### Scenario D — MCP Tool Discovery & Execution
- **Action**: Add an MCP server (e.g. GitHub or Filesystem).
- **Verification**: Available tools are discovered and listed in the UI; read tools execute safely; destructive tools trigger an approval prompt; disabled tools are never called.

### Scenario E — Agent Skill Installation & Execution
- **Action**: Import a custom skill with `SKILL.md`.
- **Verification**: Skill instructions are parsed; scripts remain disabled until approved; running the skill executes the documented workflow step by step.

### Scenario F — Hybrid RAG & Code Graph
- **Action**: Ask: "Where is user authentication handled?"
- **Verification**: RAG retrieves relevant code snippets citing exact file paths and line ranges; code graph maps callers and callees; modifying the source file updates the index.

### Scenario G — Bounded Debugging Loop
- **Action**: Introduce a syntax error or a failing test case.
- **Verification**: Agent detects test failure, isolates the stack trace, proposes a fix, verifies with a re-test, and halts gracefully if not resolved within 3 attempts.

### Scenario H — Android Build & Install Handoff
- **Action**: Build an Android APK from an Android template project.
- **Verification**: Gradle build completes inside runtime; SHA-256 checksum is computed; Android `PackageInstaller` dialog appears; user installation succeeds.

### Scenario I — Security & Path Jailing
- **Action**: Agent attempts to execute `cat /etc/shadow` or read `../../private.txt`.
- **Verification**: `PolicyEngine` intercepts and blocks the action immediately, logging a `POLICY_VIOLATION` audit event.

### Scenario J — Task Cancellation & State Recovery
- **Action**: Cancel an active code synthesis task mid-stream; kill the app process and restart.
- **Verification**: Child processes terminate cleanly; no zombie processes remain; task state resumes or reports `CANCELLED` cleanly without workspace corruption.

### Scenario K — Desktop Cross-Platform Execution
- **Action**: Launch Jarvis Desktop on Linux, Windows, or macOS.
- **Verification**: Compose desktop UI renders with full window decorations; host terminal runs via PTY; host git repositories open and build without PRoot.

### Scenario L — Multi-Key Quota Rotation & Fallback
- **Action**: Provide two Gemini API keys. Simulate a `429 RESOURCE_EXHAUSTED` error on Key #1.
- **Verification**: Gateway automatically marks Key #1 in cooldown, switches to Key #2 seamlessly, and emits a visible `RouteSwitched` timeline event without losing context.

