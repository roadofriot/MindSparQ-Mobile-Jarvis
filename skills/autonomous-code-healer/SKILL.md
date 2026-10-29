---
name: autonomous-code-healer
description: Automated stack-trace root cause analysis, diagnostic triage, and recursive test-driven self-healing loops. Inspired by morluto/rea, DietrichGebert/ponytail, and ifixai-ai/iFixAi.
version: 1.0.0
---

# Autonomous Code Healer Skill

## Purpose
Diagnoses runtime errors, compiler diagnostics, and test failures, synthesizing minimal, surgically precise fixes with bounded retry safeguards.

## Execution Protocol
1. **Trace Isolation**:
   - Parse the exact line number, exception class, and call stack frame.
   - Filter out framework internal noise to pinpoint the true fault site.
2. **Minimal Edit Rule**:
   - Never rewrite an entire module to fix a localized bug.
   - Restrict changes to the identified root cause.
3. **Bounded Repair Ceiling**:
   - Enforce a maximum of 3 automated repair cycles.
   - If tests still fail after 3 attempts, halt and request human engineering approval (`APPROVAL_REQUIRED`).
4. **Verification Gate**:
   - Run the exact targeted test suite before declaring the repair complete.
   - Verify exit code 0 and zero regressions.

