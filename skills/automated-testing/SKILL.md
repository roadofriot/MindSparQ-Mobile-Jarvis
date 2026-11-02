---
name: automated-testing
description: Design, implement, and run unit, integration, and contract tests to guarantee software correctness and prevent regressions.
tools:
  - read_file
  - write_file
  - run_command
---

# Automated Testing Skill

## Purpose
Guarantees production reliability across core domain logic, data persistence, network gateways, and state management through deterministic, repeatable test suites.

## Testing Hierarchy
1. **Unit Tests (Fast, In-Memory)**:
   - Target: Domain models, parsers, policy engines, and data mappers.
   - Requirement: Zero external I/O or mocks of standard libraries where real lightweight implementations exist.
2. **Persistence & Integration Tests**:
   - Target: SQLDelight driver operations, Keystore cryptography, and file caching.
   - Requirement: Verify ACID transactions, foreign-key cascades, and migration paths.
3. **Gateway & Protocol Contract Tests**:
   - Target: Provider model discovery responses, JSON streaming decoders, and MCP schema builders.
   - Requirement: Validate against realistic API payloads without network coupling.

## Verification Protocol
Always execute tests using hardware-conscious Gradle flags:
```bash
./gradlew testDebugUnitTest --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m"
```
Never claim a feature is complete until test outputs exit with code 0 and reports are verified.

