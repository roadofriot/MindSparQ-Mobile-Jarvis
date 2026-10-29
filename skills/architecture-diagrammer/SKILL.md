---
name: architecture-diagrammer
description: Generates clean Mermaid diagrams, system dataflows, component topologies, and architectural specifications. Inspired by cathrynlavery/diagram-design and nexu-io/open-design.
version: 1.0.0
---

# Architecture Diagrammer Skill

## Purpose
Translates complex software topologies, state machines, and microservice interactions into readable, standardized Mermaid diagrams and visual specifications.

## Conventions & Rules
1. **Supported Diagram Types**:
   - `flowchart TD` / `flowchart LR` for system pipelines and component hierarchies.
   - `sequenceDiagram` for asynchronous request/response flows.
   - `stateDiagram-v2` for finite state machines and lifecycle transitions.
   - `erDiagram` for database entity relationships.
2. **Readability Constraints**:
   - Avoid nesting beyond 3 subgraphs.
   - Quote node labels containing spaces, parentheses, or brackets (e.g. `id["Node Label (Detail)"]`).
   - Group components logically by architectural tiers (UI, Domain, Engine, Infrastructure).

