---
name: clean-code-and-refactoring
description: Perform surgical, zero-regression code refactoring, decomposing god-files, eliminating dead code, and enforcing single-responsibility principles.
tools:
  - read_file
  - edit_file
  - search_code
  - run_command
---

# Clean Code & Refactoring Skill

## Purpose
Enforces software craftsmanship, modular isolation, and file-size constraints (e.g., AGENTS.md 1,500-line ceiling) without breaking existing external behavior.

## Core Rules
1. **Preserve External Behavior**: 100% of functional contracts, UI states, and database guarantees must remain intact.
2. **Surgical Changes**: Modify only the target module and its direct dependencies. Never do blind rewrites.
3. **Evidence Before & After**:
   - Run tests/compilation before starting to confirm a green baseline.
   - Run tests/compilation after changes to prove zero regressions.

## Step-by-Step Procedure
1. **Identify Architectural Smells**:
   - God-classes or God-files (> 1,000 lines).
   - Mixed concerns (e.g., database queries inside UI composables or shell commands in view models).
   - Duplicate logic or circular dependencies.
2. **Extract Delegate / Service**:
   - Create a specialized delegate or repository with a cohesive, single responsibility.
   - Wire dependencies via clean interfaces or constructor injection.
3. **Incremental Delegation**:
   - Route caller invocations to the newly extracted delegate.
   - Deprecate or remove redundant internal logic.
4. **Verification**:
   - Run static analysis (`./gradlew lintDebug` or compiler checks).
   - Run unit test suite to verify all assertions pass.

