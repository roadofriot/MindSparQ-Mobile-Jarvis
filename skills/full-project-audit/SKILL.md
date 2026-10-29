---
name: full-project-audit
description: In-depth analysis of code quality, technical debt, security flaws, and missing tests.
tools:
  - list_dir
  - read_file
  - search_code
  - run_command
---

# Full Project Audit Skill

## Purpose
Performs a comprehensive engineering audit of an existing codebase before beginning major implementation or refactoring tasks.

## Step-by-Step Procedure
1. **Discover Project Structure**: Run `project-discovery` to establish repository topology.
2. **Examine Dependencies & Security**:
   - Inspect declared dependencies for deprecated or known-vulnerable libraries.
   - Scan code for hardcoded secrets or sensitive API credentials.
3. **Assess Test Coverage**:
   - Check for unit, integration, and UI test suites.
   - Execute existing test runners to establish the passing baseline.
4. **Identify Code Smells & Architectural Debt**:
   - Flag god-objects (files > 1,500 lines).
   - Flag tight coupling, missing abstraction layers, or untyped API boundaries.
5. **Generate Audit Report**:
   - Write output to `docs/AUDIT_REPORT.md` categorized by Critical, High, Medium, and Low severity.

