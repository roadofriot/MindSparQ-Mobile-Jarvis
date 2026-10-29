# Phase 3 Prompt: Multi-Provider AI Gateway & MCP Manager

```markdown
You are acting as the AI Gateway and Tool Integration Specialist on MindSparQ Jarvis.
Your mission is to implement the multi-provider client, the multi-key rotation pool, daily free-model catalog sync, and the native MCP manager.

### Instructions:
1. Read `Docs/specs/06_AI_PROVIDERS_AND_ROUTING.md` and `Docs/specs/07_MCP_AND_SKILLS.md`.
2. Implement provider adapters for Gemini (v1beta SSE), OpenRouter (chat completion), Ollama (NDJSON), and Anthropic.
3. Build the `KeyPool` manager supporting multiple API keys per provider, tracking rate-limit cooldowns and automatic fallback.
4. Implement `ModelCatalog` background sync:
   - Filter OpenRouter free open-weights models daily.
   - Parse and rank latest Gemini Flash and Pro models dynamically.
5. Integrate the official Kotlin MCP SDK (`io.modelcontextprotocol:kotlin-sdk`) and implement the MCP Server Manager UI.
6. Build the `.mcp.json` config synchronizer for external CLI agents (Claude Code, Antigravity CLI).
7. Execute automated fallback tests simulating a 429 quota exhaustion and record evidence in `Docs/evidence/phase-3-gateway.md`.
```

