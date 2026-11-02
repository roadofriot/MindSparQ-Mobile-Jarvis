# MindSparQ Mobile Jarvis — Enterprise Engineering Audit Report

**Audit Date:** November 2026  
**Auditor:** Principal Enterprise Software Architect & Antigravity  
**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Current Commit:** `498b170` (`main` branch)

---

## 1. Executive Summary

MindSparQ Mobile Jarvis is an autonomous, on-device AI software engineering environment engineered for mobile and low-spec developer hardware. It virtualizes an entire Linux development environment (Alpine/Ubuntu userland via PRoot) directly on Android without requiring root access.

This comprehensive audit evaluates the system against the **MindSparQ Enterprise Engineering Upgrade Specification** (`docs/ENTERPRISE_SPECIFICATION.md`). The audit confirms a healthy, compilation-verified codebase with **222 passing unit tests across 8 modules**, zero file size violations (< 1,500 lines per file ceiling), and successful physical execution on a **Samsung Galaxy Note 10+** (`RF8M926GJRY`).

The primary architectural opportunity is resolving the **KMP Dual-Layer Boundary**: harmonizing the modular domain subsystems (`com.mindsparq.jarvis.*` in `:core:*`, `:ai:*`, `:engine:*`) with the active Android UI and process virtualization layer (`com.jarves.mh.*` in `:app`), while formalizing the **Agent Skills standard** (`SKILL.md`) and dynamic **Model Context Protocol (MCP)** profiles.

---

## 2. Existing Capabilities (What Works)

| Capability Subsystem | Canonical Owner | Production Verification Evidence |
| :--- | :--- | :--- |
| **Linux Virtualization** | `com.jarves.mh.runtime.RuntimeInstaller` | Sandboxed PRoot Linux userland running inside app private storage. Zero host root required. |
| **Unified Agent CLI** | `com.jarves.mh.runtime.DshRuntimeBridge` | OpenCode CLI (`/usr/local/bin/dsh`) running headless with bidirectional JSON-RPC event bus. |
| **Relational Database** | `com.mindsparq.jarvis.core.database.JarvisDatabase` | SQLDelight schema with 14 foreign-key B-tree indexes, ACID transactions, and incremental delta message persistence. |
| **Hardware Key Vault** | `com.mindsparq.jarvis.core.policy.SecureSecretStore` | AndroidKeyStore `AES-256-GCM` key encryption. Zero plaintext secrets in SharedPreferences, logs, or UI. |
| **Security Policy Engine** | `com.mindsparq.jarvis.core.policy.PolicyEngine` | Centralized command screening in `TerminalDelegate`. Hard-denies system destruction (`rm -rf /`, fork bombs, mkfs) and enforces workspace jail. |
| **Git / GitHub Operations** | `com.jarves.mh.ui.delegate.ProjectGitDelegate` | OAuth Device Flow (`verification_uri` auto-fill), Personal Access Token auth, git clone, and authenticated remote push. |
| **Format Gateway** | `com.jarves.mh.runtime.LocalFormatGateway` | High-speed loopback HTTP proxy translating Anthropic SDK schema to OpenAI `/v1/chat/completions` protocol for OpenRouter/Gemini. |
| **Design System & UI** | `com.jarves.mh.ui.PocketWorkspaceScreen` | Apple/macOS quality dark theme tokens (`border-neutral-800`), responsive tab navigation, and interactive diff viewers. |
| **Voice Architecture** | `com.mindsparq.jarvis.engine.voice.VoiceService` | Live conversational audio with barge-in support and native audio response streaming. |

---

## 3. Incomplete, Unreliable, or Duplicate Areas

### 3.1 Domain Model Duplication
- **Observation:** `core:model:Models.kt` defines `AgentKind`, `ExecutionMode`, `TaskState`, and `ModelDescriptor`. Simultaneously, `app:model:Models.kt` defines `AgentKind`, `ProviderKind`, and `ProviderProfile`.
- **Impact:** Divergent enum constants require manual mapping across boundaries.
- **Resolution:** Harmonize models into `:core:model` as the single canonical source of truth.

### 3.2 Autonomous Task Orchestrator Gap
- **Observation:** `:engine:loop:TaskOrchestrator` implements a finite state machine (`QUEUED` $\to$ `INSPECTING` $\to$ `PLANNING` $\to$ `AWAITING_APPROVAL` $\to$ `IMPLEMENTING` $\to$ `TESTING` $\to$ `VERIFYING` $\to$ `COMPLETED`) with 9 unit tests.
- **Impact:** `MainViewModel.kt` currently coordinates tool events through ad-hoc coroutines in `ChatDelegate.kt` rather than dispatching state transitions through `TaskOrchestrator`.
- **Resolution:** Bind `ChatDelegate` and `AgentDelegate` state updates to `TaskOrchestrator` state emissions.

### 3.3 Dynamic Model Catalogue Discovery
- **Observation:** While `ModelDiscoveryService` exists in `:ai:gateway`, the UI provider setup in `:app` retains some hardcoded model collections for OpenRouter and Gemini.
- **Impact:** Deprecated models require manual app updates; free model variant additions are not discovered dynamically at runtime.
- **Resolution:** Wire `ModelDiscoveryService.refresh()` to `UnifiedAiProviderSheet.kt`.

### 3.4 MCP Server Protocol Integration
- **Observation:** `:ai:mcp` contains `McpConfigExporter`, but lacks an active runtime broker to launch and communicate with stdio MCP servers (Filesystem, GitHub, Context7, Playwright) within the PRoot Linux guest.
- **Impact:** Tool calls are currently limited to built-in CLI tools (`Bash`, `File`, `Grep`).
- **Resolution:** Implement `McpServerRuntime` to spin up scoped MCP servers inside the guest environment.

---

## 4. Current Skills, Models, and Tool Permissions

### 4.1 Current Model Support
- **OpenRouter Gateway:** Free and open-weight models (`qwen/qwen-2.5-coder-32b-instruct:free`, `cohere/north-mini-code:free`, `meta-llama/llama-3.3-70b-instruct:free`).
- **Google Gemini:** Gemini 2.5 Flash / Pro (via API key).
- **Local On-Device Models:** Supported via loopback Ollama endpoint (`127.0.0.1:11434`) and Hugging Face GGUF downloader in `OfflineModelManager`.

### 4.2 Current Tool Execution Security
- Shell executions pass through `PolicyEngine.evaluateTerminalCommand(command, workspaceRoot)`.
- Destructive commands (`git reset --hard`, `git clean -f`, `rm -rf`) require user confirmation modal (`pendingTerminalCommand`).
- Hard destructive commands (`rm -rf /`, `mkfs`, fork bombs) are unconditionally blocked with exit code 126.
- Path traversal outside `/workspace/<slug>` jail is rejected.

---

## 5. Platform Coverage & Hardware Constraints

- **Primary Tier 1 Platform:** Android 8.0+ (ARM64 & x86_64). Fully implemented, compiled, packaged into APK, and verified on physical hardware.
- **Host Build Machine:** Intel Core i3-1005G1 (dual-core) with 10 GB RAM. Gradle daemon must be constrained to `--max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"`. Android Studio emulators are forbidden on this host to avoid throttling.
- **Desktop / iOS Portability:** Desktop process runner exists in `:core:runtime:DesktopProcessRunner.kt`. iOS/macOS release compilation requires authorized macOS/Xcode build runners.
