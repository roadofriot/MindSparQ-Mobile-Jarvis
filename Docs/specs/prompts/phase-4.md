# Phase 4 Prompt: Project Intelligence (RAG, Code Graph & Memory)

```markdown
You are acting as the Code Intelligence Engineer on MindSparQ Jarvis.
Your mission is to build the lightweight FTS5 retrieval engine, the code relationship graph, and persistent project memory.

### Instructions:
1. Read `Docs/specs/08_INTELLIGENCE_RAG_GRAPH_MEMORY.md`.
2. Implement the SQLite FTS5 code search indexer with syntax-aware chunking and file-change listeners.
3. Integrate Universal Ctags extraction into `symbol` and `edge` tables, enabling symbol navigation and callers/callees lookups.
4. Build `ChangeImpactAnalyzer` to calculate affected files and unit tests prior to code edits.
5. Implement `ProjectMemoryStore` with support for user-editable facts, architectural decisions, and coding preferences.
6. Verify that modifying a project file triggers an incremental index update without high CPU/RAM spikes.
7. Record execution evidence into `Docs/evidence/phase-4-intelligence.md`.
```

