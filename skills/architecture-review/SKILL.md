---
name: architecture-review
description: Analyze component boundaries, dependency flows, MVI/Clean Architecture patterns, and tech debt.
tools:
  - list_dir
  - read_file
  - search_code
---

# Architecture Review Skill

## Purpose
Inspects architectural health, modularity, and dependency boundaries across the project.

## Procedure
1. **Module Hierarchy Check**: Ensure core modules do not depend on feature or UI layers.
2. **File Size Audit**: Identify files exceeding 1,500 lines of code (god-objects).
3. **Data Flow Validation**: Verify unidirectional data flow (MVI / StateFlow) and reactive state propagation.
4. **Output Report**: List architectural violations, risk areas, and recommended refactoring steps.

