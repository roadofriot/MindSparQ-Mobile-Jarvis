# MindSparQ Jarvis — Specification Pack (Index)

> Version 0.1 · 2026-10-29 · Status: **DRAFT — awaiting approval**
> Targets: **Android (arm64, phone/foldable)** + **PC Desktop (Linux, Windows, macOS)**

## What is MindSparQ Jarvis?

A cross-platform **AI Vibe Coding Studio**. From a phone or a PC, a user can create or import a software
project, let an AI agent audit → plan → implement → test → debug → build → install it, with every step
backed by real evidence (commands, exit codes, diffs, test output).

It is an evolution of [TechJarves Mobile-Harness](https://github.com/techjarves/Mobile-Harness) (MIT), re-architected
as a **Kotlin Multiplatform + Compose Multiplatform** application so one codebase runs on Android and Desktop.

## Documents

| # | File | Purpose | Primary reader |
|---|---|---|---|
| 00 | `00_INDEX.md` | This file; how to use the pack with an LLM | Everyone |
| 01 | [`01_AUDIT_REPORT.md`](01_AUDIT_REPORT.md) | Evidence-based audit of Mobile-Harness + Stitch designs + original instruction | Architect |
| 02 | [`02_PRD.md`](02_PRD.md) | Product Requirements: vision, personas, features, user stories, scope | Product / everyone |
| 03 | [`03_TRD.md`](03_TRD.md) | Technical Requirements: architecture, modules, runtime, agent loop, data model | Engineers / LLM agents |
| 04 | [`04_DRD.md`](04_DRD.md) | Design Requirements: Obsidian Kinetic tokens, layouts, components, mock→real mapping | Designers / UI engineers |
| 05 | [`05_SCREEN_AND_BUTTON_SPEC.md`](05_SCREEN_AND_BUTTON_SPEC.md) | Every screen, every button: action, logic, states, errors | UI engineers / QA |
| 06 | [`06_AI_PROVIDERS_AND_ROUTING.md`](06_AI_PROVIDERS_AND_ROUTING.md) | Gemini, OpenRouter (daily free-model sync), Ollama, Anthropic, multi-key pool, fallback | AI engineers |
| 07 | [`07_MCP_AND_SKILLS.md`](07_MCP_AND_SKILLS.md) | MCP client/manager, MCP registry, Agent Skills install/run, external-CLI config sync | AI engineers |
| 08 | [`08_INTELLIGENCE_RAG_GRAPH_MEMORY.md`](08_INTELLIGENCE_RAG_GRAPH_MEMORY.md) | FTS5 RAG, optional embeddings, code graph, project memory, loop engine | AI engineers |
| 09 | [`09_SECURITY_PLAN.md`](09_SECURITY_PLAN.md) | Threat model, policy engine, secrets, approvals | Security |
| 10 | [`10_TEST_PLAN_AND_ACCEPTANCE.md`](10_TEST_PLAN_AND_ACCEPTANCE.md) | Test strategy, scenarios A–L, evidence rules | QA |
| 11 | [`11_IMPLEMENTATION_PLAN.md`](11_IMPLEMENTATION_PLAN.md) | Step-by-step phases, tasks, files, exit criteria | Engineers / LLM agents |
| 12 | [`12_RISKS_AND_RESOURCE_BUDGET.md`](12_RISKS_AND_RESOURCE_BUDGET.md) | Risk register + RAM/CPU/storage/token budgets | Architect |
| 13 | [`13_ECOSYSTEM_CATALOG.md`](13_ECOSYSTEM_CATALOG.md) | Curated repos/tools from the research, and *where* each is used in Jarvis | Everyone |
| — | [`../../AGENTS.md`](../../AGENTS.md) | Rules every coding agent must follow in this repo | LLM agents |
| — | [`prompts/`](prompts/) | Copy-paste prompts, one per phase | You → LLM |

## How to use this pack with an LLM coding agent

1. Open the repository root in Antigravity / Codex / Gemini CLI / Claude Code / OpenCode.
2. The agent auto-reads `AGENTS.md`. Then paste **one** phase prompt from `prompts/phase-N.md`.
3. Do **not** paste all documents at once. Each phase prompt lists exactly which spec sections to read.
4. A phase is done only when its exit criteria in `11_IMPLEMENTATION_PLAN.md` have evidence in
   `Docs/evidence/phase-N.md`.

## Glossary

| Term | Meaning |
|---|---|
| **Jarvis Native Agent** | The built-in Kotlin agent loop (tool-calling LLM) that works with any provider via the AI Gateway |
| **External Agent** | A third-party CLI agent (Claude Code, Antigravity CLI `agy`, Gemini CLI, Codex, OpenCode) launched as a process |
| **AI Gateway** | Jarvis' provider-neutral client with key pool, routing, fallback; also exposed as a local HTTP endpoint for external agents |
| **Runtime** | Where commands execute: Android = PRoot Ubuntu (from Mobile-Harness); Desktop = host OS shell |
| **Evidence** | A persisted record of a real command, exit code, output excerpt, diff or test result |
| **Skill** | A folder with `SKILL.md` (Agent Skills standard) giving the agent a procedure |
| **MCP** | Model Context Protocol — standard for connecting tools/resources to an LLM |

