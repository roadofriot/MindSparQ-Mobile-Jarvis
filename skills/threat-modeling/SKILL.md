---
name: threat-modeling
description: Perform STRIDE threat modeling, identify security boundaries, attack vectors, privilege escalation risks, and secret leaks.
tools:
  - read_file
  - search_code
  - run_command
---

# Threat Modeling Skill

## Purpose
Systematically analyzes software architecture and implementation to identify security risks before deployment, following the STRIDE methodology (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).

## Core Security Boundaries in Jarvis
1. **Host Boundary**:
   - Intel Core i3 host vs private Linux runtime container.
   - Command screening via `PolicyEngine.isCommandAllowed`.
   - Path traversal prevention: deny commands attempting to escape project root.
2. **Secret Boundary**:
   - API keys and user credentials must only be stored in `SecretStore` (Android Keystore AES-256-GCM / OS Keychain).
   - Zero secrets in logs, transcripts, Git commits, or database plain text.
3. **Network Boundary**:
   - Outbound API calls restricted to verified provider endpoints.
   - User confirmation required before transmitting project files externally.
4. **Tool Execution Boundary**:
   - Least-privilege MCP tool profiles.
   - Destructive operations (drop tables, rm -rf, git force push) strictly gated.

## Step-by-Step Procedure
1. **Map Data Flow**: Identify all user inputs, terminal arguments, API calls, and persistent storage.
2. **Apply STRIDE Matrix**:
   - **S**poofing: Can an agent or API impersonate user credentials?
   - **T**ampering: Can project files or databases be modified out-of-band?
   - **R**epudiation: Are tool calls and command outputs logged in an immutable audit trail?
   - **I**nformation Disclosure: Can tokens or environment variables leak in stack traces or error dialogs?
   - **D**enial of Service: Can long-running loops or unconstrained builds exhaust host memory/CPU?
   - **E**levation of Privilege: Can arbitrary shell commands escape the sandbox?
3. **Output Actionable Mitigations**: Produce prioritized recommendations (`Critical`, `High`, `Medium`, `Low`).

