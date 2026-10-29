---
name: debugging-root-cause
description: Systematic error analysis, stack trace isolation, bug reproduction, and minimal targeted repair.
tools:
  - read_file
  - edit_file
  - run_command
---

# Debugging & Root Cause Analysis Skill

## Purpose
Isolates the actual underlying cause of a runtime crash, test failure, or compile error rather than guessing or repeatedly tweaking code.

## Procedure
1. **Capture Failure Artifacts**: Extract compiler output, stack traces, and exit codes.
2. **Isolate Call Site**: Identify exact file path and line number where the exception occurred.
3. **Form Hypothesis**: Differentiate symptoms from root causes (e.g. null pointer vs missing initialization).
4. **Targeted Correction**: Implement minimal fix addressing the root cause.
5. **Re-Test Verification**: Run failing test suite and ensure clean pass without regressions.

