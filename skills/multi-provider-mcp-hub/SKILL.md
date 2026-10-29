---
name: multi-provider-mcp-hub
description: Dynamic Model Context Protocol (MCP) tool integration, external CLI connectors, and multi-provider key balancing. Inspired by CherryHQ/cherry-studio and ComposioHQ/awesome-claude-skills.
version: 1.0.0
---

# Multi-Provider MCP Hub Skill

## Purpose
Standardizes MCP server integration and multi-provider routing across Google Gemini, NVIDIA NIM, OpenRouter free models, and local offline engines.

## Architecture
1. **MCP Tool Export**:
   - Generates `.mcp.json` compatible with Claude Code, Antigravity CLI, and Codex.
   - Registers standard MCP tools: filesystem, git, bash executor, and browser inspector.
2. **Multi-Key Pool Routing**:
   - Rotates active keys via LRU scheduling.
   - Automatically quarantines exhausted keys on HTTP 429 and transparently switches to secondary providers.
3. **Local Tooling**:
   - Connects guest tools installed inside PRoot (e.g. `gh`, `node`, `python3`, `git`) directly to the agent runtime.

