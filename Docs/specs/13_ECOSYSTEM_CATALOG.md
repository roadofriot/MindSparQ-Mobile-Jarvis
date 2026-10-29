# 13 — Vibe Coding Ecosystem Catalog & Integration Map

> This catalog maps the world's leading open-source repositories, developer tools, agent frameworks, MCP servers, and skills directly to their specific roles inside **MindSparQ Jarvis**.

---

## 1. Coding Agents & Core Runtimes

| Repository / Tool | What It Does | Role in MindSparQ Jarvis |
|---|---|---|
| [TechJarves Mobile-Harness](https://github.com/techjarves/Mobile-Harness) | Android-based PRoot Linux development environment and CLI agent runner | **Baseline Foundation**: Provides the PRoot ARM64 rootfs installer, JNI process spawn bridge, and Android build execution engine. |
| [Google Antigravity](https://antigravity.google/) | Google's agentic AI coding environment and CLI (`agy`) | **External Agent Option**: Integrated via `AntigravityRuntimeBridge` with user-controlled permission toggles (replacing forced `--dangerously-skip-permissions`). |
| [OpenAI Codex](https://github.com/openai/codex) | Specialized coding agent and code transformation framework | **Desktop Agent Integration**: Discovered on Desktop `PATH` and callable via Jarvis' local gateway. |
| [Gemini CLI](https://github.com/google-gemini/gemini-cli) | Terminal-based coding agent powered by Google Gemini | **CLI Alternative**: Usable directly within Jarvis Terminal tabs on host and PRoot. |
| [Claude Code](https://code.claude.com/docs/en/overview) | Anthropic's repository-aware terminal coding agent | **Ported External Agent**: Supported via `ClaudeRuntimeBridge` with auto-sync of `.mcp.json`. |
| [OpenCode](https://github.com/anomalyco/opencode) | Multi-provider terminal coding assistant | **Alternative Agent**: Supported for multi-model coding workflows on Desktop. |
| [OpenHands](https://github.com/OpenHands/OpenHands) | Autonomous software engineering environment | **Reference Pattern**: Architecture reference for multi-step task loops and Docker/sandboxed runtimes. |
| [Aider](https://github.com/Aider-AI/aider) | Git-aware AI pair programmer | **Workflow Inspiration**: Reference for minimal diff application and automatic git commit message synthesis. |

---

## 2. Model Context Protocol (MCP) Repositories

| Repository / Tool | What It Does | Role in MindSparQ Jarvis |
|---|---|---|
| [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) | Official Kotlin SDK for Model Context Protocol | **Core Native Integration**: Powers Jarvis' built-in MCP client for tool discovery and execution. |
| [Official MCP Servers](https://github.com/modelcontextprotocol/servers) | Reference MCP servers (filesystem, git, memory, etc.) | **Pre-bundled Server Suite**: Packaged as standard tools within the Jarvis MCP Registry. |
| [GitHub MCP Server](https://github.com/github/github-mcp-server) | MCP server for GitHub issues, PRs, and repository management | **GitHub Integration**: Connects Jarvis to GitHub repositories using encrypted PATs. |
| [Playwright MCP](https://github.com/microsoft/playwright) | Browser automation and DOM inspection | **Desktop Web Testing**: Powers headless browser verification and end-to-end testing on Desktop. |

---

## 3. Retrieval, Graph & Intelligence Repositories

| Repository / Tool | What It Does | Role in MindSparQ Jarvis |
|---|---|---|
| [SQLite FTS5](https://www.sqlite.org/fts5.html) | Full-text search engine built directly into SQLite | **Primary RAG Engine**: Indexes project files and documentation without consuming heavy RAM. |
| [CodeGraph](https://github.com/colbymchenry/codegraph) | Local-first code intelligence graph using AST parsers | **Architecture Reference**: Informs the design of Jarvis' `symbol` and `edge` relationship tables. |
| [Universal Ctags](https://github.com/universal-ctags/ctags) | Multilanguage source code indexer | **Symbol Indexer**: Runs inside PRoot and Desktop host to build the fast code relationship graph. |
| [Mem0](https://github.com/mem0ai/mem0) | Long-term memory management for AI applications | **Concept Reference**: Architecture model for classifying facts, decisions, and task states. |
| [LlamaIndex](https://github.com/run-llama/llama_index) | Data framework for LLM-based applications | **Design Guide**: Guidance on chunking strategies and citation provenance for code RAG. |

---

## 4. Model Routing & Local Inference

| Repository / Tool | What It Does | Role in MindSparQ Jarvis |
|---|---|---|
| [LiteLLM](https://github.com/BerriAI/litellm) | Multi-provider proxy and routing abstraction | **Gateway Architecture Reference**: Inspires Jarvis' internal `KeyPool`, retry mechanisms, and fallback chains. |
| [OpenRouter API](https://openrouter.ai/) | Unified API gateway for open-source and commercial models | **Primary Free & Fallback Provider**: Powers the Free profile via automated daily sync of `:free` open-weights models. |
| [Ollama](https://github.com/ollama/ollama) | Local model serving platform | **Local & Offline Inference**: Connected via LAN/local loopback to run local coding models without cloud fees. |
| [Google Gemini API](https://ai.google.dev/) | Multimodal frontier models (Gemini Flash, Pro) | **Primary High-Speed Provider**: Directly integrated via `v1beta` streaming APIs for rapid code generation. |

---

## 5. Agent Skills & Software Engineering Frameworks

| Repository / Tool | What It Does | Role in MindSparQ Jarvis |
|---|---|---|
| [Anthropic Agent Skills](https://github.com/anthropics/skills) | Reusable agent instructions and workflows | **Skill Standard Implementation**: Format followed by all skills inside Jarvis' `skills/` registry. |
| [Superpowers](https://github.com/obra/superpowers) | Structured software engineering agent workflows | **Workflow Source**: Basis for Jarvis' test-driven-development and architecture-review skills. |
| [VoltAgent Awesome Skills](https://github.com/VoltAgent/awesome-agent-skills) | Curated catalog of community agent skills | **Community Registry**: Source for optional installable skills via the Jarvis Skills Manager. |
| [SWE-bench](https://github.com/SWE-bench/SWE-bench) | Benchmark for evaluating coding agents on real GitHub issues | **Evaluation Standard**: Benchmark used to design Jarvis' verification and acceptance scenarios. |
| [Semgrep](https://github.com/semgrep/semgrep) | Lightweight static code analysis and security scanner | **Security Review**: Runs during release verification pipelines to detect vulnerabilities. |

