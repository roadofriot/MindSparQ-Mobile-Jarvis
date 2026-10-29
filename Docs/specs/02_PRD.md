# 02 — PRD: Product Requirements Document

## 1. Vision
> "Describe it in Nepali or English, on your phone or PC — Jarvis audits, plans, builds, tests and ships it, and proves every step."

## 2. Problem
- Vibe-coding tools are desktop-bound, single-provider, and claim "done" without evidence.
- Free-tier API keys run out mid-task; the user must manually switch providers.
- Agents forget project decisions between sessions and re-scan whole repos (token waste).
- Mobile coding apps (e.g. Mobile-Harness) lack MCP, Skills, retrieval, verification loops and a PC counterpart.

## 3. Target users (personas)
| Persona | Context | Primary need |
|---|---|---|
| **Solo vibe coder** (primary — the owner) | Low-spec laptop (i3, 10 GB) + Android phone, free/low-cost API keys | Build real apps cheaply, keep working when one key is exhausted |
| **Student / learner** | Phone only | Create & run Python/Web/Flutter projects without a PC |
| **Indie developer** | PC primary, phone for review on the go | Continue the same project from both devices via Git |

## 4. Platforms
| Platform | Runtime | Distribution |
|---|---|---|
| Android 9+ arm64 | PRoot Ubuntu (bundled installer) | GitHub Releases APK (sideload) |
| Linux x64 (Ubuntu 22.04+) | Host shell | `.deb` + AppImage |
| Windows 10/11 x64 | Host shell (PowerShell / Git-Bash if present) | `.msi` |
| macOS 13+ (arm64/x64) | Host shell | `.dmg` (unsigned in v1) |

Cross-device continuity is via **Git remotes** (GitHub), not a proprietary cloud, in v1.

## 5. Feature list (MoSCoW, v1 = Must + Should)

### 5.1 Projects & Workspace
| ID | Feature | Priority |
|---|---|---|
| F-PRJ-01 | Create project from template (Web/Vite-React, Python, Node/TS, Flutter, Android-Kotlin, C/C++, Empty) | Must |
| F-PRJ-02 | Import: Git clone (HTTPS + token), ZIP (safe extraction), folder (Desktop) / SAF folder (Android) | Must |
| F-PRJ-03 | Project dashboard: stack detection, git branch/status, last task, health | Must |
| F-PRJ-04 | Rename, archive, export ZIP, delete (to trash, 7-day restore) | Must |
| F-PRJ-05 | Checkpoints: auto before every agent write batch; restore | Must |

### 5.2 AI Workspace (Hub)
| ID | Feature | Priority |
|---|---|---|
| F-AI-01 | Chat composer with text, attachments (files/images), voice dictation | Must |
| F-AI-02 | Modes: **Ask** (read-only), **Plan** (read + write docs only), **Work** (approved edits & commands) | Must |
| F-AI-03 | Jarvis Native Agent with tools: list/read/search/write/edit files, run command, git, web fetch, MCP tools, skills | Must |
| F-AI-04 | External agents: Claude Code, Antigravity CLI, DeepSeek harness (Android, from MH); Gemini CLI, Codex, OpenCode (Desktop, if installed) | Should |
| F-AI-05 | Live activity timeline with real tool events (no fake "reasoning") | Must |
| F-AI-06 | Approval cards for risky actions (diff preview, command preview) | Must |
| F-AI-07 | Slash-command palette (`/plan`, `/audit`, `/test`, `/fix`, `/skill <name>`, `/mcp`, `/model`) | Should |
| F-AI-08 | Quick presets (user-editable prompt templates) | Should |
| F-AI-09 | Stop/cancel at any time; resume after app restart | Must |

### 5.3 Plan (Spec Studio)
| ID | Feature | Priority |
|---|---|---|
| F-PLN-01 | Generate PRD / TRD / Architecture / Implementation Plan / Test Plan / Acceptance criteria into `docs/` of the project | Must |
| F-PLN-02 | Spec tree with status (generated / edited / stale) and diff preview | Must |
| F-PLN-03 | Decision log (ADR) entries pinned to project memory | Should |
| F-PLN-04 | Task board generated from Implementation Plan (DAG with dependencies) | Should |
| F-PLN-05 | "Full project audit" skill producing `AUDIT_REPORT.md` with evidence | Must |

### 5.4 Studio (Code)
| ID | Feature | Priority |
|---|---|---|
| F-STD-01 | File tree, editor with syntax highlighting, search (FTS + grep) | Must |
| F-STD-02 | Diff viewer (unified on phone, side-by-side on Desktop), accept/reject per hunk | Must |
| F-STD-03 | Terminal (multiple sessions, copy, history; PTY on Desktop; line-mode on Android) | Must |
| F-STD-04 | Git panel: status, stage, commit, branch, pull, push (confirm), log | Must |
| F-STD-05 | Architecture graph (modules/files/symbols from code graph) | Should |
| F-STD-06 | Tests panel: discovered suites, run, parsed pass/fail, jump to failure | Must |

### 5.5 Sim (Run & Ship)
| ID | Feature | Priority |
|---|---|---|
| F-SIM-01 | Run dev server; WebView preview with viewport presets (phone/tablet/desktop) | Must |
| F-SIM-02 | Console log capture from preview | Should |
| F-SIM-03 | Android: build APK → checksum → system installer → launch | Must |
| F-SIM-04 | Desktop: run app natively; Android via user's ADB devices | Should |
| F-SIM-05 | Pipeline view: Verify (lint+test) → Build → Package → Deliver (GitHub Release upload, opt-in) | Should |

### 5.6 Settings & Integrations
| ID | Feature | Priority |
|---|---|---|
| F-SET-01 | Providers: Gemini, OpenRouter, Ollama, Anthropic, OpenAI-compatible custom; **multiple keys per provider** | Must |
| F-SET-02 | Routing profiles & fallback chain editor; key health dashboard | Must |
| F-SET-03 | OpenRouter free open-source model daily sync | Must |
| F-SET-04 | Gemini & Anthropic & Ollama live model discovery | Must |
| F-SET-05 | MCP server manager (registry search, add, permissions, test) | Must |
| F-SET-06 | Skills manager (install from GitHub/registry/ZIP, inspect, enable per project) | Must |
| F-SET-07 | Policy: command allowlist, auto-approve levels, network access | Must |
| F-SET-08 | Runtime (Android): install/repair, toolchain packs, storage usage | Must |
| F-SET-09 | Appearance: dark/light, font size; language English/Nepali | Should |
| F-SET-10 | Usage: tokens & estimated cost per provider/day | Should |

### 5.7 Intelligence
| ID | Feature | Priority |
|---|---|---|
| F-INT-01 | Project index (FTS5) auto-refreshed on file change | Must |
| F-INT-02 | Code graph (symbols, imports, references) | Should |
| F-INT-03 | Project memory (facts, decisions, task state) editable by user | Must |
| F-INT-04 | Embeddings (provider API) hybrid search | Could |

### 5.8 Voice
| ID | Feature | Priority |
|---|---|---|
| F-VOX-01 | Dictation (Android SpeechRecognizer ne-NP/en-US; Desktop: Gemini audio transcription) | Should |
| F-VOX-02 | Spoken progress (TTS) | Could |
| F-VOX-03 | Live voice conversation (Gemini Live API) | Won't (v1) → P7 |

### Out of scope v1 (Won't)
iOS builds · offline LLM bundled in app (Ollama on LAN/PC is supported) · team collaboration/cloud sync · Play Store release · arbitrary-app UI automation.

## 6. Key user stories (acceptance in doc 10)
1. *As a vibe coder, I type "Build a Nepali-English expense tracker in Flutter" → Jarvis creates the project, writes PRD/plan, I approve, it implements, runs `flutter test`, shows evidence.*
2. *When my Gemini key #1 hits 429, the task continues on key #2, then OpenRouter free model, without losing context; the timeline shows each switch.*
3. *I add the GitHub MCP server from the registry with read-only tools; Jarvis lists my issues; write tools stay disabled.*
4. *I install the `superpowers` skills pack, enable "test-driven-development" for one project only.*
5. *On my phone I clone the repo I started on PC, ask Jarvis to fix a failing test, and push.*
6. *I connect Ollama running on my PC (`http://192.168.1.10:11434`) from my phone and use `qwen3-coder` when offline from the internet but on LAN.*

## 7. Success metrics
| Metric | Target |
|---|---|
| Task completes despite a provider failure (scenario K) | 100 % when ≥1 healthy route exists |
| Completion reports with evidence for every claim | 100 % |
| Cold start (Desktop / Android) | < 2.5 s / < 3.5 s |
| Idle RAM (Desktop / Android, no agent running) | < 450 MB / < 250 MB |
| Crash-free sessions | ≥ 99 % |

## 8. Non-functional requirements
Security (doc 09) · Accessibility: 48 dp targets on Android, TalkBack labels, contrast ≥ 4.5:1 · i18n: en, ne · Offline-tolerant UI (all local features work without internet; AI features show clear "offline" state) · Data stays on device except calls to configured providers.

