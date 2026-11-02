---
name: production-readiness
description: Execute pre-release production readiness audit covering compilation, tests, memory leaks, dependency licenses, crash handlers, and release signing.
tools:
  - read_file
  - run_command
---

# Production Readiness Skill

## Purpose
Guarantees that a codebase satisfies enterprise release criteria before committing, releasing, or deploying to end users or production stores.

## Pre-Flight Quality Gate Checklist
1. **Compilation & Static Analysis**:
   - Zero compiler errors across all supported targets.
   - Lint checks exit with code 0 (`./gradlew lintDebug` or linter equivalent).
2. **Automated Test Suite**:
   - 100% of unit and integration tests passing.
   - Zero flakiness or ignored critical tests.
3. **Hardware & Resource Limits**:
   - Gradle invoked with `--no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"`.
   - No runaway background threads, memory leaks, or unconstrained watchers.
   - APK size and memory footprint within verified bounds.
4. **Security & Secrets**:
   - Zero hardcoded API keys or test credentials in git tree (`git diff` clean).
   - SecretStore properly backed by OS Keystore / Keychain.
5. **UI & State Completeness**:
   - No mock buttons, dead handlers, or `// TODO` stubs.
   - Full empty, loading, error, and recovery states for every user-facing screen.
6. **Reproducible Release Build**:
   - Release artifact built and signed with verified exit code 0.

