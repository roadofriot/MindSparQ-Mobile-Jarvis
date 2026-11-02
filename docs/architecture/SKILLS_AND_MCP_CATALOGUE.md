# MindSparQ Mobile Jarvis — Skills and MCP Catalogue

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Standards Compliance:** Open Agent Skills Standard (`agentskills.io`) & Model Context Protocol (`modelcontextprotocol.io`)

---

## 1. Agent Skills Standard Architecture

Skills are organized into standardized, isolated directories under `~/.jarvis/skills/<category>/<skill-name>/`:

```text
~/.jarvis/skills/architecture/repository-audit/
├── SKILL.md              # Mandatory: Frontmatter, metadata, activation triggers, workflow
├── scripts/              # Optional: Sandboxed scripts executed during workflow
├── templates/            # Optional: Markdown/code templates used for output
└── references/           # Optional: Domain documentation and validation schemas
```

### Frontmatter Schema (`SKILL.md`)
```yaml
---
name: "repository-audit"
description: "Comprehensive code inspection, health check, dead code removal, and security audit"
version: "2.1.0"
category: "architecture"
source: "https://github.com/mindsparq/agent-skills"
license: "MIT"
permissions:
  filesystem: "read-only"
  network: "none"
  shell: "restricted"
---
```

---

## 2. Core Curated Skills Matrix

| Category | Skill Identifier | Purpose & Activation Signal | Source / Provenance |
| :--- | :--- | :--- | :--- |
| **A. Product Planning** | `product-discovery` | Analyze plain-language prompt, extract user personas, identify MVP scope. | MindSparQ Standard / AgentSkills |
| | `prd-and-brd-authoring` | Generate structured PRD/BRD with functional & non-functional requirements. | MindSparQ Standard |
| | `user-story-acceptance` | Translate requirements into testable Gherkin acceptance criteria. | MindSparQ Standard |
| **B. Architecture** | `repository-audit` | Strict read-only inspection of repository structure, dead code, and bottlenecks. | MindSparQ Core |
| | `system-architecture` | Choose language, framework, database, and modular boundaries. | MindSparQ Core |
| | `technical-decision-records` | Author and track Architecture Decision Records (ADRs). | MindSparQ Core |
| **C. UX/UI & Design** | `design-system-engineering` | Enforce design tokens (spacing, typography, dark/light contrast). | Apple HIG / M3 Standard |
| | `accessibility-wcag` | Audit touch targets, focus states, screen reader semantics, and contrast. | WCAG 2.2 Standard |
| **D. Implementation** | `clean-code-and-refactoring`| Modularize god-files, preserve 100% external behavior, enforce 1500-line ceiling. | MindSparQ Standard |
| | `flutter-engineering` | Multiplatform development on Ubuntu/Linux/Android with low RAM footprint. | Flutter Open Standard |
| | `native-android-engineering`| Jetpack Compose, Kotlin Multiplatform, Gradle optimization. | Android Open Source Project |
| **E. AI & Agentic** | `model-routing-and-fallback`| Dynamically match tasks to optimal models with zero silent downgrades. | MindSparQ AI Gateway |
| | `sandboxed-execution` | Manage PRoot Linux userland jail and enforce path isolation. | MindSparQ Security |
| **F. Quality & Security** | `automated-testing` | Generate and execute unit, integration, and E2E test suites. | MindSparQ QA |
| | `threat-modeling` | Audit OWASP Top 10, MASVS, secrets leakage, and prompt injection vectors. | OWASP Standard |
| **G. Delivery** | `deployment-and-packaging` | Assemble release APKs, verify signing, generate release notes and changelogs. | MindSparQ Release |

---

## 3. Curated MCP Server Catalogue & Task Profiles

MCP servers run as isolated child processes communicating over stdio. Tool access is governed by task-specific profiles:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          MCP TASK PROFILES                             │
├───────────────┬────────────────────────────────────────────────────────┤
│ Profile       │ Active MCP Tool Servers Enabled                        │
├───────────────┼────────────────────────────────────────────────────────┤
│ `coding`      │ Filesystem (scoped), Git / GitHub, Context7            │
│ `testing`     │ Filesystem (scoped), Playwright, Test Result Parser    │
│ `design`      │ Figma MCP (read-only), Design Tokens Resolver          │
│ `research`    │ Context7, Web Fetch (SSRF-protected allowlist)         │
│ `database`    │ Database Schema Inspector (read-only query mode)       │
│ `release`     │ GitHub Releases, Package Validator                     │
└───────────────┴────────────────────────────────────────────────────────┘
```

### Server Security Policy
1. **Filesystem MCP:** Strictly jailed to `/workspace/<project_slug>`. Any path escaping the directory boundary is terminated immediately.
2. **Database MCP:** Read-only by default. Destructive operations (DROP, ALTER, TRUNCATE) require explicit modal confirmation.
3. **Fetch MCP:** Enforces strict SSRF filtering (blocks loopback `127.0.0.1`, cloud metadata `169.254.169.254`, and private subnets).

