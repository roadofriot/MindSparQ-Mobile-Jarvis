# Phase 2 Prompt: Plan/Work Engine, Loop State Machine & Core Skills

```markdown
You are acting as the AI Agent Systems Engineer on MindSparQ Jarvis.
Your mission is to implement the execution state machine (Ask/Plan/Work), the bounded debugging loop, and the Agent Skills framework.

### Instructions:
1. Read `Docs/specs/07_MCP_AND_SKILLS.md` and `Docs/specs/08_INTELLIGENCE_RAG_GRAPH_MEMORY.md`.
2. Implement `TaskOrchestrator` state machine (`QUEUED` -> `INSPECTING` -> `PLANNING` -> `IMPLEMENTING` -> `TESTING` -> `COMPLETED`).
3. Build the `ApprovalCard` UI flow, ensuring diffs and high-risk commands pause for user confirmation.
4. Implement test discovery and output parsing for JUnit, Pytest, Vitest, and Flutter test suites.
5. Create the 10 initial foundational skills under `skills/` using the standard `SKILL.md` format.
6. Verify bounded retry behavior: ensure tasks halt after 3 failed test repair attempts.
7. Record execution evidence into `Docs/evidence/phase-2-loop-engine.md`.
```

