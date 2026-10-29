---
name: token-context-optimization
description: Manage context window budgets, summarize long histories, prune repetitive tools, and reduce API token waste.
tools:
  - read_file
---

# Token & Context Optimization Skill

## Purpose
Keeps LLM prompts compact, efficient, and cost-effective, preventing context window overflow and high API token costs.

## Procedure
1. **Context Breakdown**: Estimate tokens consumed by system instructions, memory, retrieved code, and chat history.
2. **Selective Snippet Ingestion**: Include only relevant lines with path:line references instead of full files.
3. **Rolling Conversation Compaction**: Summarize turns older than 5 rounds into concise factual bullet points.
4. **Tool Call Result Truncation**: Truncate long terminal logs or diffs exceeding 2,000 characters before prompt injection.
5. **Output Efficiency Metric**: Report estimated tokens saved and remaining context budget.

