# MindSparQ Jarvis — Rules for Coding Agents

> Every AI agent operating in this repository must strictly adhere to these rules.

---

## 1. Core Engineering Principles
1. **Never Falsify Claims**: Never claim a task or feature is complete without verifiable evidence (successful build exit code 0, executed test outputs, real git diffs).
2. **Resource-Conscious Execution**: The host environment is constrained (Intel i3 / 10 GB RAM).
   - Never run unconstrained background daemons or watchers.
   - Always invoke Gradle with `--no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"`.
   - Never launch Android emulators on the host machine.
3. **Audit Before Modifying**: Never rewrite an existing component without inspecting its current implementation, its dependent modules, and documenting the justification.
4. **Preserve Working Features**: Retain functional runtime, installer, and bridge code ported from Mobile-Harness.

---

## 2. Code Quality & Architecture
- **Package Name**: `com.mindsparq.jarvis`.
- **Modularity**: Code is structured into Kotlin Multiplatform layers (`core`, `engine`, `ai`, `feature`, `apps`).
- **Dependency Guardrail**: Features must never be imported by core, ai, or engine modules.
- **File Size Ceiling**: No single source file shall exceed 1,500 lines of code. Large files must be split by responsibility.

---

## 3. Security & Secret Protection
- Never write API keys, user tokens, or secrets to plaintext configuration files, database logs, terminal history, or git commits.
- All secrets must pass through `SecretStore` (backed by Android Keystore on mobile and OS keychain on desktop).
- All command executions must be screened by `PolicyEngine`. Path traversal attempts outside project workspaces are strictly denied.

