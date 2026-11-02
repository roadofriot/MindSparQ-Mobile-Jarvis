# MindSparQ Mobile Jarvis — Phased Implementation Roadmap

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Execution Owner:** Antigravity / Senior Software Engineering Team

---

## Roadmap Overview & Phases

```
┌────────────────────────────────────────────────────────────────────────┐
│ Phase 0: Audit & Governance Baseline                       [COMPLETED] │
│ • Complete audit report, capability gap matrix, and spec integration. │
├────────────────────────────────────────────────────────────────────────┤
│ Phase 1: Database & Performance Hardening                  [COMPLETED] │
│ • SQLDelight indexing, delta upsert persistence, zero "null" bubbles.  │
├────────────────────────────────────────────────────────────────────────┤
│ Phase 2: Security & PolicyEngine Binding                   [COMPLETED] │
│ • TerminalDelegate screening, destruction denial, jail boundary check. │
├────────────────────────────────────────────────────────────────────────┤
│ Phase 3: Delegate Decoupling & Line Ceiling Compliance     [COMPLETED] │
│ • ProjectGitDelegate extraction (ProjectDelegate reduced to 964 lines)│
├────────────────────────────────────────────────────────────────────────┤
│ Phase 4: Model Gateway & Dynamic Catalogue Discovery     [NEXT PHASE]  │
│ • Connect ModelDiscoveryService to UI, eliminate hardcoded model lists.│
├────────────────────────────────────────────────────────────────────────┤
│ Phase 5: Agent Skills Standard & MCP Profiles             [PHASE 5]    │
│ • Formalize ~/.jarvis/skills/ directory with SKILL.md specs.           │
├────────────────────────────────────────────────────────────────────────┤
│ Phase 6: Orchestrator Finite State Machine                [PHASE 6]    │
│ • Bind :engine:loop:TaskOrchestrator to MainViewModel execution cycle.│
├────────────────────────────────────────────────────────────────────────┤
│ Phase 7: Cross-Platform Handoff & Release Pipelines       [PHASE 7]    │
│ • GitHub Actions CI/CD matrix for reproducible APK & desktop artifacts│
└────────────────────────────────────────────────────────────────────────┘
```

---

## Detailed Milestone Deliverables

### Milestone 1: Audit & Foundation (Phases 0–3) — **100% DONE**
- [x] Integrate master specification at `docs/ENTERPRISE_SPECIFICATION.md`.
- [x] Author all 9 canonical architecture governance documents in `docs/architecture/`.
- [x] Add 14 B-Tree indexes and delta upsert queries to `JarvisDatabase.sq`.
- [x] Integrate `PolicyEngine` into `TerminalDelegate` with destruction denial.
- [x] Extract `ProjectGitDelegate.kt` (582 lines) and keep all Kotlin files under 1,500 lines.
- [x] Eliminate `"null null"` tool-call bubbles and filter ghost records.
- [x] Verify on physical Samsung Galaxy Note 10+ (`RF8M926GJRY`).

### Milestone 2: Model Gateway & Dynamic Catalogue (Phase 4) — **100% DONE**
- [x] Connect `ModelDiscoveryService` and `ModelRegistry` to `UnifiedAiProviderSheet.kt`.
- [x] Dynamically populate OpenRouter, Gemini, Ollama, and NIM models from runtime metadata and cache.
- [x] Eliminate hardcoded/fictitious model lists, supporting live discovery and capability filtering (All, Free Tier, Tools, Reasoning).
- [x] Relational model catalogue persistence in SQLDelight `model` table via `JarvisDatabaseBridge`.

### Milestone 3: Skills Ecosystem & MCP Integration (Phase 5) — **100% DONE**
- [x] Structure standard skills folders under `skills/` with `SKILL.md` metadata:
  - `technical-decision-records`
  - `clean-code-and-refactoring`
  - `automated-testing`
  - `threat-modeling`
  - `production-readiness`
  - plus 17 existing canonical skills (22 total).
- [x] Upgrade `SkillManager.kt` with multi-directory discovery (workspace, `~/.jarvis/skills`, bundled), recursive scanning, and task-class filtering.
- [x] Implement scoped MCP task profiles (`CODING`, `TESTING`, `DESIGN`, `RESEARCH`, `DATABASE`, `ALL`) in `McpConfigExporter.kt` to enforce least-privilege tool execution.
- [x] Verified unit tests and live hardware deployment on physical Samsung Galaxy Note 10+ (`RF8M926GJRY`).

### Milestone 4: Autonomous State Graph (Phase 6)
- [ ] Drive agent execution via `TaskOrchestrator` state machine (`QUEUED` $\to$ `COMPLETED`).
- [ ] Persist task state transitions in `task` and `evidence` tables in `JarvisDatabase.sq`.
- [ ] Support task interruption, state resumption, and step recovery.
