---
name: requirements-and-planning
description: Transform user intent into structured PRD, TRD, architecture, and dependency-aware task graphs.
tools:
  - list_dir
  - read_file
  - write_file
---

# Requirements & Planning Skill

## Purpose
Synthesizes professional Product Requirement Documents (PRD), Technical Requirement Documents (TRD), and task breakdown graphs before writing any code.

## Procedure
1. **Analyze User Intent**: Break down prompt into functional goals and non-functional requirements (performance, offline support, security).
2. **Inspect Existing Docs**: Check `docs/` for existing architectural guidelines or constraints.
3. **Draft Architecture Spec**:
   - Define data models and state boundaries.
   - Map external dependencies and tool requirements.
4. **Generate Task Graph**:
   - Produce sequential, dependency-aware steps (`- [ ] Step N: ... deps: [...]`).
5. **Write Documents**: Output documents to `docs/PRD.md`, `docs/TRD.md`, and `docs/IMPLEMENTATION_PLAN.md`.

