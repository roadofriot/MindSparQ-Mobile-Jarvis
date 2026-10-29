---
name: security-review
description: Scan source code for exposed credentials, path traversal flaws, unsafe commands, and vulnerable dependencies.
tools:
  - list_dir
  - read_file
  - search_code
---

# Security Review Skill

## Purpose
Identifies vulnerabilities, credentials in code, and dangerous execution patterns before commits or releases.

## Procedure
1. **Secret Scanning**: Check for API keys (`AIza...`, `sk-...`, `ghp_...`, Bearer tokens) in source files.
2. **Path Traversal Check**: Ensure user-controlled file paths are validated against directory traversal (`../`).
3. **Execution Guardrails**: Inspect shell invocation sites for unvalidated user input or command injection vulnerabilities.
4. **Dependency Audit**: Review declared package versions against known security advisories.
5. **Output Security Findings**: Categorize by Critical, High, Medium, and Low severity.

