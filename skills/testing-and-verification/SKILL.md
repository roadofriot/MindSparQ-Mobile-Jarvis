---
name: testing-and-verification
description: Automated test discovery, execution, test suite expansion, and regression prevention.
tools:
  - list_dir
  - read_file
  - run_command
---

# Testing & Verification Skill

## Purpose
Discovers existing project test runners, executes tests, parses output into structured evidence, and adds missing regression tests.

## Procedure
1. **Discover Test Runners**: Detect JUnit, Pytest, Vitest, Jest, or Flutter test commands.
2. **Execute Targeted Suites**: Run tests for changed components before executing the full suite.
3. **Parse Results**: Extract passed, failed, and skipped counts.
4. **Author Missing Tests**: Create reproduction test cases for newly reported bugs or modified features.
5. **Output Verification Evidence**: Produce verifiable evidence records for task completion.

