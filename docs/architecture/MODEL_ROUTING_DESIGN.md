# MindSparQ Mobile Jarvis — Universal Model Routing Design

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Providers:** Google Gemini API · OpenRouter · Local On-Device (Ollama / GGUF)

---

## 1. Universal Model Gateway Architecture

All AI requests pass through `com.mindsparq.jarvis.ai.gateway.AiGateway` and `LocalFormatGateway.kt`:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AI REQUEST DISPATCH                             │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                    TASK-AWARE ROUTING POLICY                           │
│  Classifies task: Fast Edit · Deep Code · Architecture · Voice · Test  │
└────────────────────────────────────┬───────────────────────────────────┘
                                     │
┌────────────────────────────────────▼───────────────────────────────────┐
│                     KEY POOL & RATE LIMIT MANAGER                      │
│  Selects healthy key with lowest tokens used today · Cooldown tracking │
└───────────────────────┬──────────────────────────┬─────────────────────┘
                        │                          │
┌───────────────────────▼────────┐        ┌────────▼─────────────────────┐
│       GOOGLE GEMINI API        │        │          OPENROUTER          │
│  Gemini 2.5 Flash / 3.x Flash  │        │  Verified Free Open Models   │
│  Direct SDK / REST Streaming   │        │  Qwen 2.5 Coder · Llama 3.3  │
└────────────────────────────────┘        └────────┬─────────────────────┘
                                                   │
                                          ┌────────▼─────────────────────┐
                                          │      LOCAL ON-DEVICE         │
                                          │  127.0.0.1:11434 (Ollama)    │
                                          │  Quantized GGUF Offline Engine│
                                          └──────────────────────────────┘
```

---

## 2. Dynamic Model Catalogue & Discovery

Model identifiers are never permanently hardcoded. The application executes a dynamic discovery routine at startup and on demand:

1. **Provider Sync:** Queries `https://openrouter.ai/api/v1/models` and Google `models.list`.
2. **Metadata Normalization:** Normalizes context window length, pricing, tool-call support, reasoning capabilities, and free-tier flags into `ModelDescriptor`.
3. **Free-Model Prioritization:** Automatically surfaces verified free models (e.g., `qwen/qwen-2.5-coder-32b-instruct:free`, `cohere/north-mini-code:free`) in user dropdowns.
4. **Last-Known-Good Cache:** If the device is offline or the provider returns 5xx errors, Jarvis falls back to cached metadata in `JarvisDatabase.sq` (`model` table).

---

## 3. Task-Aware Routing Matrix

| Task Category | Primary Preferred Route | Secondary Fallback Route | Offline Route |
| :--- | :--- | :--- | :--- |
| **Simple Chat & Intent Routing** | Fast OpenRouter Free (`north-mini-code:free`) | Gemini 2.5 Flash | Local Ollama (`qwen2.5:3b`) |
| **Code Generation & Editing** | OpenRouter Coder (`qwen-2.5-coder-32b-instruct:free`) | Gemini 2.5 Flash | Local Ollama (`qwen2.5-coder:7b`) |
| **Architecture & Root Cause Debugging** | Gemini 2.5 Pro / Claude 3.7 Sonnet | OpenRouter Large (`llama-3.3-70b-instruct:free`) | None (Transparent failure message) |
| **Conversational Voice Audio** | Gemini Live Audio / Google Speech API | Gemini 2.5 Flash | On-device Android TTS/STT |
| **Offline Autonomous Execution** | Local Ollama (`127.0.0.1:11434`) | None | Local Ollama |

---

## 4. Transparent Failover & Fallback Protocol

When an active session encounters an error (HTTP 429 rate limit, 502 gateway error, or credit exhaustion):
1. **Key Rotation:** If multiple keys are configured for the provider, switch to the next healthy key in `KeyPoolManager`.
2. **Model Fallback:** If the primary model is throttled, fall back to an equivalent model within the same capability class.
3. **Provider Failover:** Cascade from Google Gemini $\to$ OpenRouter $\to$ Local Ollama.
4. **Zero Silent Downgrades:** If a complex reasoning task cannot be fulfilled without a high-tier model, Jarvis displays an honest notice rather than silently producing broken code with an inferior model.
