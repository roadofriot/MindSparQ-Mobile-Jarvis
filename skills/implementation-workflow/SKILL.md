---
name: implementation-workflow
description: Step-by-step TDD implementation applying minimal, targeted diffs and immediate test verification.
tools:
  - read_file
  - edit_file
  - write_file
  - run_command
---

# Implementation Workflow Skill

## Purpose
Executes planned code changes using small, verifiable steps while preventing broad, destructive rewrites.

## Procedure
1. **Target Selection**: Pick a single task from the active implementation plan.
2. **Pre-edit Inspection**: Read existing source files and dependent interfaces completely.
3. **Draft Minimal Diff**: Apply targeted file edits without removing unrelated comments or functions.
4. **Immediate Verification**: Run relevant unit tests to verify the specific modification.
5. **Report Outcome**: Output a clear summary of files changed, diffs applied, and test results.

