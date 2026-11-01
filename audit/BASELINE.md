# MindSparQ Mobile Jarvis — System Baseline & Verification Report (Phase 0)

**Date:** November 1, 2026  
**Git Baseline Commit:** `68e98076ef0908c28a467875ec8c36fc37e666cc`  
**Active Branch:** `main` (clean working tree)  
**Host Environment:** Ubuntu Linux (Intel Core i3-1005G1 @ 1.20GHz x 4, 10 GB RAM)  
**Target Hardware:**
- Samsung Galaxy Note 10+ (`RF8M926GJRY`, Android 12, 11.2 GB RAM, arm64-v8a)
- Xiaomi Redmi Note 10S (`6TDUGIVWVK8LHY85`, Android 12, MIUI)

---

## 1. Gradle Modules & Dependency Inventory

The project consists of 15 configured Gradle modules:

| Module | Type | Target Namespace | Responsibility |
| :--- | :--- | :--- | :--- |
| `:app` | Android Application | `com.mindsparq.jarvis` (`com.jarves.mh`) | Android UI, Runtime Bridges, ViewModels, System Services |
| `:core:model` | Android/KMP Library | `com.mindsparq.jarvis.core.model` | Shared domain data structures, enums, quotas, billing tiers |
| `:core:database` | Android/KMP Library | `com.mindsparq.jarvis.core.database` | SQLDelight SQLite relational schema & database driver |
| `:core:policy` | Android/KMP Library | `com.mindsparq.jarvis.core.policy` | `PolicyEngine`, `SecretStore`, Android Keystore integration |
| `:core:designsystem`| Android Library | `com.mindsparq.jarvis.core.designsystem` | Design tokens, Apple/macOS minimal components, responsive layout |
| `:core:runtime` | Android/KMP Library | `com.mindsparq.jarvis.core.runtime` | `DeviceMemoryGovernor`, `WakeLockManager`, `DesktopProcessRunner` |
| `:ai:gateway` | Android/KMP Library | `com.mindsparq.jarvis.ai.gateway` | `AiGateway`, `KeyPoolManager`, `CostPolicyEngine`, `ModelRegistry` |
| `:ai:skills` | Android/KMP Library | `com.mindsparq.jarvis.ai.skills` | Dynamic skill management & tool registration |
| `:ai:mcp` | Android/KMP Library | `com.mindsparq.jarvis.ai.mcp` | Model Context Protocol config export & server integration |
| `:engine:loop` | Android/KMP Library | `com.mindsparq.jarvis.engine.loop` | `TaskOrchestrator`, autonomous repair loop, step budget |
| `:engine:index` | Android/KMP Library | `com.mindsparq.jarvis.engine.index` | Codebase chunking & FTS full-text indexing |
| `:engine:graph` | Android/KMP Library | `com.mindsparq.jarvis.engine.graph` | AST symbol graphs & change impact analysis |
| `:engine:memory`| Android/KMP Library | `com.mindsparq.jarvis.engine.memory` | Project vector/relational semantic memory store |
| `:engine:test` | Android/KMP Library | `com.mindsparq.jarvis.engine.test` | Test execution parsing & failure classification |
| `:engine:voice` | Android/KMP Library | `com.mindsparq.jarvis.engine.voice` | Native voice services & audio processing |

---

## 2. File Size Ceiling Violations (`AGENTS.md` Rule 2)

Mandatory rule: *No single source file shall exceed 1,500 lines of code.*

| File | Current Lines | Status | Recommended Target |
| :--- | :---: | :---: | :---: |
| `app/.../ui/PocketDevApp.kt` | **8,666** | 🔴 Violates ceiling by 5.7x | Decompose into ~10 domain screen files (< 800 lines each) |
| `app/.../ui/MainViewModel.kt` | **6,133** | 🔴 Violates ceiling by 4.1x | Decompose into scoped domain delegates (< 1,000 lines each) |
| `app/.../ui/AgentScreen.kt` | **2,288** | 🟠 Violates ceiling by 1.5x | Extract sub-composables into `agent/` package |
| `app/.../runtime/RuntimeInstaller.kt` | **2,074** | 🟠 Violates ceiling by 1.3x | Extract distribution tar/package logic |
| `app/.../ui/SettingsScreenModern.kt` | 1,252 | 🟢 Compliant | Retain as is |
| `app/.../ui/UnifiedAiProviderSheet.kt` | 1,236 | 🟢 Compliant | Retain as is |
| `app/.../runtime/ClaudeRuntimeBridge.kt` | 1,137 | 🟢 Compliant | Retain as is |
| `app/.../runtime/DshRuntimeBridge.kt` | 1,070 | 🟢 Compliant | Retain as is |
| `app/.../ui/LiveVoiceProjectArchitect.kt` | 1,018 | 🟢 Compliant | Retain as is |

---

## 3. Verified Test & Build Baseline

### 3.1 Unit Test Execution
Command:
```bash
./gradlew :app:testOnlineDebugUnitTest :ai:gateway:testDebugUnitTest :core:policy:testReleaseUnitTest --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"
```
**Outcome:** `BUILD SUCCESSFUL in 1m 5s` (149 actionable tasks, 0 test failures).
- `KeyPoolAndFallbackTest`: 5/5 passed (Quota scopes, 401 invalidation, cross-provider fallback, cost protection).
- `PolicyEngineTest`: Passed.
- `SecureSecretStoreTest`: Passed.
- `AppUnitTests`: Passed.

### 3.2 Physical Device Installation & Live Verification
- **Target Device:** Samsung Galaxy Note 10+ (`RF8M926GJRY`).
- **APK Installed:** `app/build/outputs/apk/online/debug/app-online-debug.apk` (75 MB).
- **Execution:** `am start -n com.mindsparq.jarvis/com.jarves.mh.MainActivity` launched and verified in foreground.
- **Diagnostics:** Zero runtime crashes or logcat exceptions.
- **Hardware Profile Detected:** `Samsung SCV45` (8-Core arm64-v8a, 11.2 GB RAM, 146 GB free storage).

---

## 4. Architectural Findings Summary

1. **Storage Bifurcation:** SQLDelight SQLite database exists in `:core:database` (`JarvisDatabase.sq`) but `:app` currently persists projects, chat history, and active state in `pocket_preferences` (Android SharedPreferences) as serialized JSON.
2. **Secret Store Divergence:** `ApiKeyVault` in `com.jarves.mh.data` uses direct Keystore cipher with SharedPreferences, whereas `SecureSecretStore` in `:core:policy` implements the `SecretStore` interface with `EncryptedSharedPreferences`.
3. **Gateway Redundancy:** `LocalFormatGateway.kt` (loopback proxy with key pool) and `OpenRouterRoutingGateway.kt` (standalone loopback proxy) operate separately.
4. **Live Voice Isolation:** `GeminiLiveVoiceClient.kt` executes raw HTTP requests directly to Gemini/OpenRouter, bypassing `AiGateway` and `KeyPoolManager`.

