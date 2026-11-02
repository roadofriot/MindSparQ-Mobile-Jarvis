# MindSparQ Mobile Jarvis — Capability Gap Matrix

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Classification System:** `IMPLEMENTED` | `PARTIAL` | `MISSING` | `BLOCKED`

---

## 1. Six-Layer Capability Matrix

| Capability Layer | Specification Requirement | Current State in Jarvis | Status | Remediation Plan |
| :--- | :--- | :--- | :---: | :--- |
| **1. Product & Requirements Intelligence** | Plain-language idea discovery, PRD/BRD generation, user stories, acceptance criteria, MVP vs Phase 2 scope breakdown. | Templates generate starter projects. `LiveVoiceProjectArchitect.kt` provides conversational project scaffolding. | `PARTIAL` | Integrate standard Agent Skills: `product-discovery`, `prd-and-brd-authoring`, `user-story-and-acceptance-criteria`. |
| **2. Architecture & Technical Design** | Codebase inspection before recommendations, Architecture Decision Records (ADRs), modular boundaries, offline-first sync patterns. | Integrity check and AST analysis exist in `IntegrityScheduler.kt`. Codebase chunking in `CodeChunker.kt`. | `PARTIAL` | Formalize ADR generation skill (`technical-decision-records`) and automated repository audit skill (`repository-audit`). |
| **3. Design & Experience Engineering** | Design tokens, Apple HIG & Material Design 3 parity, responsive layouts, accessibility WCAG 2.2, complete states (empty, loading, error, offline). | Jetpack Compose theme in `Theme.kt`, `ResponsiveLayout.kt`, and `PocketWorkspaceScreen.kt` provide macOS-quality dark tokens. | `IMPLEMENTED` | Expand visual regression testing and WCAG contrast check automated suites in `:core:designsystem:test`. |
| **4. Autonomous Software Engineering** | Controlled workspace sandboxing, safe dependency install, linters/formatters, test execution, root-cause diagnosis. | PRoot Linux userland virtualization, OpenCode CLI, terminal execution, `PolicyEngine` command screening, and `TestResultParser.kt`. | `IMPLEMENTED` | Bind `:engine:loop:TaskOrchestrator` to the active `MainViewModel` execution cycle. |
| **5. Quality, Security & Release** | Threat modeling, secrets in keystore, reproducible builds, release notes, pre-release gates, regression suites. | Android KeyStore AES-256-GCM (`SecureSecretStore`), git tag release notes, and automated unit tests. | `PARTIAL` | Enforce Pre-Release Engineering Gates checklist (`docs/architecture/ACCEPTANCE_TEST_PLAN.md`). |
| **6. Learning & Capability Maintenance** | Agent Skills standard (`SKILL.md`), curated MCP discovery, dynamic model catalogue refresh, safe rollback. | Model catalog discovery exists in `:ai:gateway`. MCP config export exists in `:ai:mcp`. | `PARTIAL` | Standardize local skills directory into `~/.jarvis/skills/` following open Agent Skills convention. |

---

## 2. Model & Gateway Matrix

| Feature | Requirement | Current Status | Remediation |
| :--- | :--- | :---: | :--- |
| **Gemini Multi-Key** | Support multiple Gemini keys with rotation, cooldown, and label tracking. | `IMPLEMENTED` | Managed via `KeyPoolManager` and `UnifiedAiProviderSheet.kt`. |
| **OpenRouter Dynamic Catalogue** | Runtime fetch of models, pricing, and free tier collections. | `PARTIAL` | `ModelDiscoveryService` fetches models; connect directly to provider selector UI. |
| **Local On-Device Models** | Loopback Ollama and quantized GGUF execution. | `IMPLEMENTED` | Loopback `127.0.0.1:11434` tested and supported. |
| **Format Translation** | Fast proxy between Anthropic & OpenAI protocols. | `IMPLEMENTED` | `LocalFormatGateway.kt` running on port 14455 with zero `"null"` token leaks. |
| **Cost & Token Tracking** | Per-key token budgets and daily usage metrics. | `IMPLEMENTED` | `CostPolicyEngine.kt` and `usage_daily` table in `JarvisDatabase.sq`. |

---

## 3. Tool & Protocol Matrix

| Integration | Protocol | Current Status | Target Architecture |
| :--- | :--- | :---: | :--- |
| **Filesystem MCP** | Stdio / JSON-RPC | `PARTIAL` | Scoped to `/workspace/<project_slug>`. Deny access to host root and Android private data. |
| **GitHub MCP / Git** | Native CLI + REST | `IMPLEMENTED` | `ProjectGitDelegate.kt` handles OAuth Device flow, clone, and remote push. |
| **Playwright MCP** | Browser Automation | `MISSING` | Wire Playwright MCP via headless Chromium in Linux PRoot or host browser bridge. |
| **Context7 MCP** | Documentation Resolver | `MISSING` | Integrate Context7 MCP endpoint for real-time library documentation lookups. |
| **Figma MCP** | Design-to-Code Context | `MISSING` | Optional design profile tool for importing design tokens and components. |
