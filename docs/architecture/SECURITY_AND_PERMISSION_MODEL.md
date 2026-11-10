# MindSparQ Mobile Jarvis — Security & Permission Model

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Compliance Standards:** OWASP Mobile Application Security Verification Standard (MASVS) & NIST SSDF

---

## 1. Security Architecture Principles

1. **Zero Trust in Model Output:** Generated code, shell scripts, and tool calls are treated as untrusted until screened by deterministic security filters outside the model.
2. **Hardware-Backed Keystore:** All API keys and authentication tokens are encrypted at rest using Android KeyStore `AES-256-GCM`. Plaintext keys never touch SharedPreferences, logs, or UI layouts.
3. **Loopback Gateway Isolation:** External AI providers connect via a local loopback gateway (`LocalFormatGateway` on `127.0.0.1:14455`). Untrusted guest processes in PRoot never see real API bearer tokens.
4. **Workspace Jail Enforcement:** Claude Code permission requests are screened against `/workspace/<project_slug>` and its canonical host workspace. Out-of-workspace file paths and explicit shell path traversal are denied.

---

## 2. Deterministic Agent Tool Screening (`PolicyEngine`)

Claude Code's permission-hook requests are checked by `PolicyEngine.evaluateAgentTool` before the bridge writes an allow response:

| Request | Policy action |
| :--- | :---: |
| File read/write/edit whose resolved host path is inside the project workspace | `ALLOW` |
| `pwd`, `ls`, `git status`, `git diff`, `git log`, or `git branch` | `ALLOW` |
| Other commands, compound commands, or unknown tools | `ASK` |
| File paths outside the project, paths escaping through symlinks, explicit `..` traversal, or absolute shell paths outside the guest workspace | `DENY` |
| Hard-denied destructive commands | `DENY` |

The interactive terminal has its own `evaluateTerminalCommand` path. The above agent-tool allowlist must not be interpreted as a complete shell sandbox.

### Runtime coverage
- The autonomous workflow is restricted to Claude Code, whose permission-hook requests are integrated with the app's approval UI.
- The planner and reviewer use the configured provider API; they do not receive project tools. The reviewer receives a bounded project diff.
- DeepSeek Harness and Antigravity currently use their existing runtime permission modes and are not covered by this Claude permission hook. They are not eligible as autonomous implementers.
- The workflow is off by default, does not install agents, and can make extra provider calls. Planning/review require a key-based API provider; Claude subscription-login tokens alone are not used for those direct API calls.

---

## 3. Human-in-the-Loop Approval Boundaries

For Claude Code, ordinary file edits within the project are permitted automatically. Commands outside the small read-only allowlist and any unrecognized tool request wait for an explicit decision; explicit out-of-workspace paths and hard-denied patterns cannot be approved through that request. Other coding-agent bridges are not covered by these guarantees.
