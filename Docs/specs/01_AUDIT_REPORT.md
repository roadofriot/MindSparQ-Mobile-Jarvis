# 01 — Audit Report

> Inputs audited: (A) TechJarves Mobile-Harness @ `96b1da08` (2026-10-11), (B) `Docs/Stitch/*` (7 screens + desktop variants + `DESIGN.md`), (C) the user's original instruction ("Vibe Coding Master Research" + "MindSparQ Mobile Vibe Coding Studio" master prompt).

---

## A. Mobile-Harness codebase

### A.1 Size & shape

| Metric | Value |
|---|---|
| Kotlin + C source | ~28,500 lines |
| Unit tests | 98 `@Test` (JUnit4, JVM only) |
| Instrumented tests / CI | none / none (`.github/` absent) |
| Largest files | `ui/PocketDevApp.kt` 7,378 L · `ui/MainViewModel.kt` 5,400 L · `runtime/RuntimeInstaller.kt` 1,997 L |
| Min / target SDK | 28 / **28** (direct APK) · 36 (Play flavour, runtime unvalidated) |
| ABI | arm64-v8a only |
| Licences | App MIT · PRoot GPL-2.0 · talloc LGPL-3.0+ · libandroid-shmem BSD-3 |

### A.2 Reusable assets (to port into `androidMain`)

| Asset | Files | Reuse decision |
|---|---|---|
| PRoot runtime install/repair | `runtime/RuntimeInstaller.kt`, `RuntimeSetupService.kt`, `RootfsRepair.kt`, `RuntimeCompatibility.kt` | **Port as-is**, wrap behind `RuntimeHost` interface |
| Native spawn (JNI) | `cpp/pocket_spawn.c`, `pocket_launcher.c`, `runtime/NativeSpawnProcess.kt` | **Port as-is** |
| Foreground execution service | `runtime/RuntimeExecutionService.kt` | Port |
| External agent bridges | `ClaudeRuntimeBridge.kt`, `DshRuntimeBridge.kt`, `AntigravityRuntimeBridge.kt`, `AntigravityAuthController.kt`, `AgentDriver.kt` | Port; make platform-neutral parts common |
| Format gateway (OpenAI⇄Anthropic) | `runtime/LocalFormatGateway.kt`, `OpenRouterRoutingGateway.kt` | Port into `ai-gateway` module (JVM-common) |
| Secrets | `data/ApiKeyVault.kt` (Android Keystore) | Port; add Desktop impl |
| Checkpoints | `runtime/WorkspaceCheckpoints.kt` | Port to common (pure file ops) |
| Android build / install / templates | `AndroidBuildSupport.kt`, `AndroidAppInstaller.kt`, `AndroidAppInstallReceiver.kt`, `AndroidProjectTemplateGenerator.kt` | Port |
| Bounded reads, local network helpers | `BoundedFileReads.kt`, `network/LocalNetwork.kt` | Port to common |
| Unit tests | `app/src/test/**` | Port alongside code; must stay green |

### A.3 Not reused (replaced)

| Asset | Reason |
|---|---|
| `ui/PocketDevApp.kt`, `ui/MainViewModel.kt`, `SettingsScreenModern.kt`, `AgentScreen.kt`, `TerminalScreen.kt` | God objects; Android-only; new design system (Obsidian Kinetic) and multiplatform UI required. UI is **re-implemented** in `commonMain`, behaviour parity verified against a feature checklist (see `11_IMPLEMENTATION_PLAN.md` P2). |
| `data/AppPreferences.kt` (SharedPreferences JSON) | Replaced by SQLDelight DB; one-time migration importer kept for users of Mobile-Harness data is **not needed** (new applicationId → fresh install). |
| `update/AppUpdater.kt` | Points at techjarves releases; replaced with MindSparQ release manifest. |

### A.4 Gap list

| ID | Gap | Severity | Evidence |
|---|---|---|---|
| G1 | God-object UI / VM | High | file sizes above |
| G2 | No MCP support | High | `grep -ri mcp app/src/main` → 0 |
| G3 | No Skills support | High | `grep -ri skill` → 0 |
| G4 | No relational DB | Medium | `AppPreferences.kt` L245 `projects_json` |
| G5 | No RAG / index / graph / memory | Medium | no sqlite/fts/embedding/tree-sitter |
| G6 | Antigravity always `--dangerously-skip-permissions` | High (security) | `AntigravityRuntimeBridge.kt` L219, L680 |
| G7 | No direct Gemini API provider, no Ollama, no multi-key pool, no cross-provider fallback | High (for this product) | `model/Models.kt` `ProviderKind` |
| G8 | No built-in agent loop — all intelligence delegated to external CLIs | High | `RuntimeBridge` only launches CLIs |
| G9 | No plan/approve/work state machine, no test parser, no bounded repair loop | Medium | — |
| G10 | No CI | Medium | — |
| G11 | No PTY (TUI apps render badly) | Low | README "process bridge rather than a complete PTY" |
| G12 | No Desktop target | High (for this product) | Android-only Gradle |
| G13 | Runtime bundles hosted on upstream GitHub releases | Medium | `build.gradle.kts` `runtimeReleaseBaseUrl` |
| G14 | No Nepali locale, no voice | Low | `res/values-*` |

### A.5 Hard platform constraints

1. **targetSdk 28** is required for exec of PRoot binaries from app data on Android 10+ (W^X). ⇒ Android distribution is **sideload / GitHub Releases / F-Droid**, not Google Play.
2. **PRoot is not a security sandbox.** Code run inside can read all app-private data. ⇒ secrets never placed on the PRoot filesystem; policy engine gates commands.
3. **No emulator/Docker/KVM on phone.** "Simulator" = WebView preview, APK install+launch, and (Desktop) user's own emulator/devices via ADB.
4. **iOS builds impossible** without macOS + Xcode. Desktop-on-macOS may run them in a later phase only if the host has Xcode; out of scope for v1.

---

## B. Stitch designs (`Docs/Stitch`)

### B.1 Inventory

| Screen (mobile) | Desktop variant | Purpose in mock |
|---|---|---|
| `jarvis_command_hub` | `jarvis_command_hub_studio_desktop` | Prompt composer, quick presets, active sub-agents, voice |
| `jarvis_project_planner_spec_studio` | `..._desktop` | Spec synthesis (PRD/TRD/architecture docs), model chips, spec tree |
| `jarvis_architecture_studio` | (in command hub desktop) | Node graph of components, diff/branch/export |
| `jarvis_live_simulator` | `jarvis_live_simulator_deploy_hub_desktop` | Device frames, hot reload, touch inspector |
| `jarvis_deploy_hub` | (same desktop) | Pipeline stages, log stream, deploy targets |
| `jarvis_profile_settings` | — | Identity, model routing & quotas, preferences, storage |
| `obsidian_kinetic/DESIGN.md` | — | Design system tokens |

Bottom navigation in mocks: **Hub · Plan · Studio · Sim · Settings** — adopted.

> [!WARNING]
> 9 of 10 `screen.png` files are 28-byte placeholders containing the text `<FIFE Image failed to fetch>`; only `jarvis_project_planner_spec_studio_desktop/screen.png` (1280×1024) is a real image. The HTML is complete and was used as the source of truth. Please re-export the PNGs from Stitch if visual reference images are wanted in the repo.

### B.2 Mock content that is fake or infeasible (must not ship as-is)

| Mock element | Problem | Shipping replacement |
|---|---|---|
| iPhone 16 Pro / iPad frames, "Deploy to TestFlight / App Store", Expo EAS iOS | iOS impossible on Android/Linux/Windows | Android APK, Web build, Desktop build targets; device frames only as WebView viewport presets |
| "Touch Inspector" editing padding of a running app | Cannot read view trees of arbitrary apps | Web preview: DOM inspector via WebView JS bridge (own preview only). Android: optional Phase 7 via user-enabled Wireless Debugging + `uiautomator dump` |
| "latency 18ms", "SYNC 98.4%", "AST v4.2", "p95 184ms", "2,480 toks/s" | Fabricated telemetry | Real measured latency/tokens from AI Gateway; hidden if unknown |
| "Sovereign Pro · Tier 4", "AIRGAP ENCLAVE", ed25519 identity, "Revoke Cryptographic Session" | No such backend | Local profile (name, avatar, GitHub account link via OAuth device flow) |
| "Gemini 1.5 Pro", "Claude 3.5 Sonnet", "Llama 3.3 70B" chips | Outdated, hard-coded | Live model list from discovery (see doc 06) |
| "3/3 Sub-Agents running" | Expensive on phone | Single active agent by default; optional reviewer pass; parallel agents Desktop-only (P7) |
| "GIT PUSH // FORCE LEASE" as a settings button | Dangerous in settings | Moved to Git sheet with confirmation |
| "Execute Arbitrage Swap", NovaPay/Nexus Capital content | Sample app content | Replaced with the user's real project preview |

---

## C. The original instruction (user's research + master prompt)

### C.1 What is adopted
- Audit-first, evidence-before-claims, PASS/FAIL/BLOCKED/NOT RUN statuses.
- Layers: Skills · Agents/Models · MCP/Tools · RAG/Memory/Graph · Verification/Governance.
- Plan mode / Work mode with approval gates; bounded test-debug-repair loop.
- Least-privilege MCP, no auto-running skill scripts, secrets never in logs.
- Phased delivery with exit criteria; scenarios A–J (extended to A–L here for Desktop and fallback).

### C.2 What is changed, and why
| Original | Change | Reason |
|---|---|---|
| "Evolve Mobile-Harness" (Android only) | KMP + Compose Multiplatform, Android **and** Desktop | New requirement: Mobile + PC |
| Rely on external agent CLIs | Add **Jarvis Native Agent** as the default | Required for multi-key fallback across Gemini/OpenRouter/Ollama; external CLIs can't fail over between providers |
| MCP integration "using an SDK" | Two layers: (1) native MCP client in Jarvis (official Kotlin SDK), (2) config sync to external CLIs | Native agent needs tools; external agents already speak MCP |
| RAG via vector DB | SQLite FTS5 first, embeddings optional via provider API | Phone RAM/battery |
| Code graph via Tree-sitter | ctags/tree-sitter CLI inside runtime → SQLite | Avoid JNI port of tree-sitter grammars |
| Model list in prompt | Live discovery + daily sync, no hard-coded IDs | Models change weekly |

