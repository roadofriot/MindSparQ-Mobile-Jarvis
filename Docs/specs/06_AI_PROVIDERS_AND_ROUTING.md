# 06 — AI Providers, Multi-Key Pool, Routing & Fallback

## 1. Goals
1. Use the **latest fast & efficient** models without hard-coding model IDs.
2. **Many API keys per provider**; when one is exhausted/erroring, switch automatically.
3. When a whole provider fails, **fall back to the next provider** in the route chain, preserving the task context.
4. **OpenRouter: sync free, open-weights models daily**; only those appear in the Free profile.
5. **Ollama** (local PC, LAN, or Ollama Cloud) supported now; it is also the path to offline later.

> [!IMPORTANT]
> Model names change monthly. Web research on 2026-10-29 reported IDs such as `gemini-3.8-flash`, `gemini-3.5-flash-lite` and a new Pro-tier model, but these are **unverified**. Jarvis therefore never ships a fixed model list: it discovers models from each provider's `models` endpoint at runtime and ranks them (§3). Seeds below are used only before the first successful discovery.

## 2. Providers

| Kind | Endpoint(s) | Auth | Discovery | Chat API used | Notes |
|---|---|---|---|---|---|
| **GEMINI** | `https://generativelanguage.googleapis.com/v1beta` | `x-goog-api-key` | `GET /models?pageSize=1000` → filter `supportedGenerationMethods ∋ generateContent` | `POST /models/{m}:streamGenerateContent?alt=sse` (native) | Must round-trip `thoughtSignature` parts in function-calling turns (Gemini 3+); `thinkingConfig` (`thinkingLevel` or `thinkingBudget` depending on model family); aliases `gemini-flash-latest`, `gemini-flash-lite-latest`, `gemini-pro-latest` used as seeds |
| **OPENROUTER** | `https://openrouter.ai/api/v1` | `Authorization: Bearer` | `GET /models` (public) | `POST /chat/completions` (OpenAI format, `stream: true`) | Send `HTTP-Referer` + `X-Title: MindSparQ Jarvis`; `GET /key` for remaining credit/limits |
| **OLLAMA** | default `http://127.0.0.1:11434` (Desktop), user URL e.g. `http://192.168.1.10:11434` (phone→PC on LAN), `https://ollama.com` (Cloud, API key) | none / Bearer (Cloud) | `GET /api/tags` + `POST /api/show` (`capabilities`: tools, vision, thinking) | `POST /api/chat` (native, streaming NDJSON, tools) | Desktop: "Start Ollama" hint if not running; Android: LAN/cloud only in v1 |
| **ANTHROPIC** | `https://api.anthropic.com/v1` | `x-api-key`, `anthropic-version` | `GET /models` | `POST /messages` (SSE) | Claude Sonnet 4.x default seed |
| **OPENAI_COMPAT** | user base URL (DeepSeek, Kimi, NVIDIA NIM, Groq, Cerebras, LM Studio, llama.cpp server, vLLM…) | Bearer | `GET /models` if available, else manual model ID | `POST /chat/completions` | capability flags set manually when undiscoverable |

Adapters live in `ai/gateway/providers/*`. Each maps `ChatRequest` ⇄ native format, normalises stream events into `ChatEvent` and errors into `ProviderError` (§4).

### 2.1 Seeds (pre-discovery only)
| Profile | Seed chain |
|---|---|
| Fast | `gemini/gemini-flash-latest` → `openrouter/openrouter/free` → `ollama/<first tools-capable>` |
| Smart | `gemini/gemini-pro-latest` → `anthropic/<latest sonnet>` → `gemini/gemini-flash-latest` |
| Cheap (summaries, commit msgs) | `gemini/gemini-flash-lite-latest` → Fast chain |
| Free | OpenRouter free open-weights ranked list → `openrouter/openrouter/free` → Ollama |
| Local | Ollama models only |

## 3. Model catalog & ranking (`ModelCatalog`)

### 3.1 Refresh triggers
- App start if last refresh > 24 h; provider added/edited; manual "Refresh models"; **daily background job** (Android WorkManager `PeriodicWorkRequest(24h, flex 2h, requiresNetwork)`; Desktop coroutine scheduler while app open + on start).

### 3.2 Gemini ranking ("latest fast & efficient")
```
parse id with /^gemini-(\d+(?:\.\d+)?)-(flash-lite|flash|pro)(?:-(preview|exp)[\w-]*)?$/
exclude: embedding, imagen, tts, live/audio-only, aqa, gemma (listed separately)
tier = flash | flash-lite | pro ; version = float
exclude preview/exp unless Settings › Models › "Allow preview models"
best[tier] = max(version), tie → stable over preview, then newest
Fast profile head = best[flash]; Cheap = best[flash-lite]; Smart = best[pro]
```
If a higher version later appears, the profile head updates automatically and a notification says "Fast model updated: X → Y" (user can pin the old one).

### 3.3 OpenRouter free open-source daily sync
```
GET https://openrouter.ai/api/v1/models
keep m where
   m.pricing.prompt == "0" && m.pricing.completion == "0"     // or id endsWith ":free"
   && m.hugging_face_id is not blank                          // open weights published on HF
   && "tools" in m.supported_parameters                       // needed by the agent (setting can relax for Ask mode)
   && m.context_length >= 32_000
rank by: created desc, then context_length desc
upsert into model(is_free=1, open_weights=1, last_seen_at=now)
models not seen for 2 consecutive syncs → hidden (pinned models keep but show "unavailable")
Free profile chain = top N (default 5) ranked + "openrouter/free" router as last resort
```
Sync result row in Settings › Models: "2026-10-29 06:00 · 23 free open-weights models · 3 new · 1 removed".

### 3.4 Ollama
`/api/tags` every refresh; for each model `/api/show` → capabilities. Tools-capable models eligible for Work mode; others Ask-only.

## 4. Error classification

| Class | Detected from | Key action | Request action |
|---|---|---|---|
| `RATE_LIMITED` | HTTP 429; Gemini `RESOURCE_EXHAUSTED` w/ per-minute quota; OpenRouter 429 | key → COOLING until `retry-after` / Gemini `RetryInfo.retryDelay` / default 60 s | next key immediately |
| `QUOTA_EXHAUSTED` | Gemini 429 with daily quota violation (`QuotaFailure` mentions per-day); OpenRouter 402 / daily free cap; Anthropic credit error | key → EXHAUSTED until next reset (Gemini: midnight America/Los_Angeles; OpenRouter: 00:00 UTC; unknown: +24 h) | next key |
| `AUTH` | 401, 403 (`API_KEY_INVALID`, `PERMISSION_DENIED`) | key → INVALID (user must fix) + notification | next key |
| `OVERLOADED` / `SERVER` | 500, 502, 503, 504, Anthropic 529 | key untouched; provider circuit +1 failure | retry same key once after 1–3 s jittered backoff, then next provider |
| `TIMEOUT` / `NETWORK` | connect 10 s, first-byte 60 s (thinking models 120 s), idle-stream 45 s | — | retry once, then next provider |
| `BAD_REQUEST` | 400 (schema, context too long) | — | context-too-long → compact context & retry same model once; other 400 → **no fallback**, surface error (fallback would hide a bug) |
| `CONTENT_BLOCKED` | safety finish reasons | — | surface to user; optional "retry on other provider" button |
| `MODEL_GONE` | 404 model not found | model → hidden | next model in chain; trigger catalog refresh |

## 5. Key pool & router algorithm

### 5.1 Key states
`HEALTHY → COOLING(until) → HEALTHY` · `HEALTHY → EXHAUSTED(until reset) → HEALTHY` · `* → INVALID` (manual) · `DISABLED` (user).

Selection within a provider: among HEALTHY keys, lowest `priority` first, then **least-recently-used** (spreads free-tier quotas); per-key in-flight limit 2.

### 5.2 Circuit breaker per provider
5 failures of class SERVER/TIMEOUT within 2 min → OPEN for 60 s (skipped); then HALF_OPEN (one probe request).

### 5.3 Route execution
```kotlin
suspend fun FlowCollector<GatewayEvent>.execute(task: TaskContext, req: ChatRequest) {
    val chain = router.chainFor(task)          // pinned model (chat) + profile chain, filtered by
                                               // capability needs: tools? vision? ctx >= estimate?
    val tried = mutableListOf<Attempt>()
    for (target in chain) {
        if (breaker.isOpen(target.provider)) { tried += skipped(target); continue }
        val keys = keyPool.candidates(target.provider)          // empty for Ollama w/o auth = [null]
        for (key in keys) {
            var sameKeyRetries = 0
            while (true) {
                val outcome = attempt(target, key, req.adaptedFor(target.model))
                when {
                    outcome.success -> { usage.record(target, key, outcome.usage); return }
                    outcome.error.class == BAD_REQUEST -> throw outcome.error       // no fallback
                    outcome.error.class in setOf(RATE_LIMITED, QUOTA_EXHAUSTED, AUTH) -> {
                        keyPool.mark(key, outcome.error); emit(RouteSwitched(target, key, outcome.error)); break }
                    outcome.error.retryableSameKey && sameKeyRetries++ < 1 -> delay(jitter(1000, 3000))
                    else -> { breaker.fail(target.provider); emit(RouteSwitched(...)); continue@for_target }
                }
            }
        }
    }
    throw NoRouteAvailable(tried)   // UI: card listing each attempt + "Open Providers"
}
```

### 5.4 Streaming failover rule
- Failure **before the first token / tool call** → transparent fallback.
- Failure **mid-stream** → partial output kept in the timeline marked "interrupted", request re-sent to the next route with the same messages (partial is *not* fed back, to avoid corrupted tool calls). Emitted as `RouteSwitched(midStream=true)`.
- Tool-call turns are atomic: a tool is executed only after the complete tool-call object is received.

### 5.5 Context portability
Messages are stored provider-neutral. On switch, adapter re-encodes history for the new provider. Provider-specific opaque data (Gemini `thoughtSignature`, Anthropic thinking blocks) is kept per message but **dropped when switching provider** (and the thinking text is not replayed). Images are dropped with a note if the new model lacks vision. If history exceeds the new model's context → summarise oldest turns with the Cheap profile first.

### 5.6 Local gateway for external agents
`LocalGatewayServer` (Ktor server CIO, bound to 127.0.0.1, random port, per-session bearer) exposes `/v1/messages` (Anthropic) and `/v1/chat/completions` (OpenAI). External CLIs get env `ANTHROPIC_BASE_URL`/`OPENAI_BASE_URL` pointing to it, so Claude Code / OpenCode / Codex also use the key pool & fallback. Builds on MH `LocalFormatGateway`.

## 6. Settings UI (buttons)

### 6.1 Providers & Keys
| ID | Control | Logic |
|---|---|---|
| PV-01 | Provider list with health dot & key count | `provider` + `api_key` |
| PV-02 | "+ Add provider" → type picker (Gemini / OpenRouter / Ollama / Anthropic / OpenAI-compatible) | creates disabled provider, opens editor |
| PV-03 | Editor: display name, base URL (prefilled), enabled toggle | — |
| PV-04 | Keys list: label, masked value, state pill, cooldown countdown, today's requests/tokens, priority drag handle | — |
| PV-05 | "+ Add key" (paste, or **bulk paste** one per line) | each saved to SecretStore → `secret_ref`; never shown again in full |
| PV-06 | Key overflow: Test, Rename, Disable/Enable, Reset state, Delete (confirm) | Test = `listModels` + 1-token chat; result shown with latency |
| PV-07 | "Test all keys" | sequential with 500 ms gap (avoid self-rate-limit) |
| PV-08 | Ollama: "Detect on LAN" (Android) | probes user-entered host only — no network scanning |
| PV-09 | OpenRouter: "Check credits" | `GET /key` → limit, usage, is_free_tier |

### 6.2 Routing
| ID | Control | Logic |
|---|---|---|
| RO-01 | Profiles list (Auto, Fast, Smart, Cheap, Free, Local + custom) | `route_profile` |
| RO-02 | Chain editor: ordered list of `provider/model` (or "best flash", "best free" dynamic slots), drag to reorder, add/remove | stored `chain_json`; dynamic slots resolved by catalog at request time |
| RO-03 | Task-class mapping: Chat, Plan/Spec, Code edit, Review, Summary/Commit msg, Embeddings → profile | `ModelRouter` uses `task_class` |
| RO-04 | "Auto" profile rule: Plan/Review → Smart; Code edit → Fast; Summary → Cheap; no keys → Free; offline → Local | — |
| RO-05 | Toggles: allow preview models; allow paid OpenRouter models; max cost/day (USD, stops paid routes when hit) | — |
| RO-06 | "Simulate failure" (developer option) | forces 429 on head route to verify fallback (used in scenario K) |

### 6.3 Models
| ID | Control | Logic |
|---|---|---|
| MO-01 | Filters: provider, free, open-weights, tools, vision, thinking | — |
| MO-02 | Row: id, ctx, capability icons, "new" badge (first seen < 7 days), pin, hide | — |
| MO-03 | Last sync per provider + "Sync now" | §3 |

## 7. Usage & cost
Tokens from provider usage fields (Gemini `usageMetadata`, OpenAI `usage`, Anthropic `usage`, Ollama `prompt_eval_count/eval_count`). Cost computed only when pricing known (OpenRouter pricing fields; user-entered for others). Shown as "≈" with source.

## 8. Tests (must exist)
- Ktor `MockEngine` fixtures per provider for: stream text, tool call, 429 per-minute, 429 per-day, 401, 503, timeout, 404 model.
- Router tests: key rotation order, cooldown expiry, exhausted reset time, breaker open/half-open, mid-stream switch, BAD_REQUEST no-fallback, context compaction on switch, thoughtSignature dropped on cross-provider switch.
- Catalog tests: Gemini version ranking incl. preview exclusion; OpenRouter filter with recorded `/models` JSON fixture.

