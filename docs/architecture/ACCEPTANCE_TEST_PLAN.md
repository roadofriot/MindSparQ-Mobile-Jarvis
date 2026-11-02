# MindSparQ Mobile Jarvis — Enterprise Acceptance Test Plan

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Classification System:** `PASS` | `FAIL` | `BLOCKED` | `NOT TESTED` | `NOT APPLICABLE`

---

## 1. Pre-Release Engineering Quality Gates

| Quality Gate | Verification Protocol & Command | Pass Criteria | Current Status |
| :--- | :--- | :--- | :---: |
| **Gate 1: Compilation & Lint** | `./gradlew checkKotlinGradlePluginConfigurationErrors` | Exit code 0, zero compilation errors across all 8 modules. | **`PASS`** |
| **Gate 2: Unit Test Suite** | `./gradlew testDebugUnitTest --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"` | All 222 unit tests pass across `:core`, `:ai`, `:engine`, and `:app`. | **`PASS`** |
| **Gate 3: Security & Policy** | `./gradlew :core:policy:test` | Hard deny on `rm -rf /`, fork bombs, mkfs. Path jail enforcement verified. | **`PASS`** |
| **Gate 4: Database ACID & Indexing** | `./gradlew :core:database:test` | SQLDelight schema executes with indexes; delta upserts preserve messages. | **`PASS`** |
| **Gate 5: APK Assembly** | `./gradlew assembleOnlineDebug --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"` | Produces valid signed `app-online-debug.apk`. | **`PASS`** |
| **Gate 6: Physical Hardware Execution** | `adb -s RF8M926GJRY install -r <apk>` & `am start` | App installs, launches to foreground, and renders UI without crashes. | **`PASS`** |
| **Gate 7: File Size Ceiling** | `find . -name "*.kt" -exec wc -l {} + \| sort -rn \| head -n 1` | No single Kotlin file exceeds the 1,500-line ceiling in `AGENTS.md`. | **`PASS`** |
| **Gate 8: Memory & Resource Overhead** | Memory check on physical device / PRoot | App memory footprint stays within constrained hardware budget (< 500 MB RAM). | **`PASS`** |

---

## 2. Core Functional Acceptance Scenarios

### Scenario 1: New Project Scaffolding
- **Given:** User taps "New Project" and selects Android Kotlin template.
- **When:** Project files are generated in `/workspace/<slug>`.
- **Then:** Git initializes, initial commit is recorded, and `JarvisDatabase` saves the project row.
- **Status:** **`PASS`**

### Scenario 2: Agent Conversation & Tool Execution
- **Given:** User asks OpenCode CLI to inspect files.
- **When:** Model executes `find` and `grep` tool calls.
- **Then:** Tool results are formatted cleanly in the timeline with zero `"null"` bubbles.
- **Status:** **`PASS`**

### Scenario 3: Malicious / Destructive Command Rejection
- **Given:** A command or agent prompt contains `rm -rf /` or `:(){ :|:& };:`.
- **When:** `PolicyEngine.evaluateTerminalCommand` evaluates the string.
- **Then:** Command is immediately denied with exit code 126 and toast warning.
- **Status:** **`PASS`**

### Scenario 4: Path Jail Breach Prevention
- **Given:** A script attempts to read `/etc/shadow` or `../../../../system`.
- **When:** `PolicyEngine.evaluatePathAccess` verifies the target path against the project root.
- **Then:** Traversal attempt is blocked with `PolicyAction.DENY`.
- **Status:** **`PASS`**

### Scenario 5: Secret Protection & Encryption
- **Given:** User configures a Gemini or OpenRouter API key.
- **When:** Key is saved.
- **Then:** Key is stored in AndroidKeyStore `AES-256-GCM` and masked in all UI components.
- **Status:** **`PASS`**
