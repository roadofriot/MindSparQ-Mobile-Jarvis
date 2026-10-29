---
name: multi-agent-orchestrator
description: Coordinates specialized autonomous agents (Planner, Coder, Auditor, Tester, Reviewer) in a bounded, verified pipeline. Inspired by paperclipai/paperclip and VoltAgent/awesome-openclaw-skills.
version: 1.0.0
---

# Multi-Agent Orchestrator Skill

## Purpose
Orchestrates autonomous subagents with clearly separated responsibilities, preventing context bloat and hallucinated task completion.

## Pipeline Lifecycle
```
[User Request]
       │
       ▼
[Planner Agent] ──▶ Produces Phased Implementation Plan
       │
       ▼
[Coder Agent]   ──▶ Applies Minimal Surgical Code Diffs
       │
       ▼
[Tester Agent]  ──▶ Executes Targeted Test Suite (Exit Code 0)
       │
       ▼
[Auditor Agent] ──▶ Verifies Security, Memory, & No Plaintext Secrets
       │
       ▼
[Completion]
```

## Guardrails
- Every agent must produce verifiable artifacts (diffs, test logs, checksums).
- Subagents operate within isolated contexts to conserve token windows and memory.

