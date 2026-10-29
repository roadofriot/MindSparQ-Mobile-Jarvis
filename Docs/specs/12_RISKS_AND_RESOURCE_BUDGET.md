# 12 — Risks & Resource Budgets

> This document establishes hard operational budgets for CPU, memory, storage, and API tokens to ensure MindSparQ Jarvis runs reliably on constrained hardware (dual-core i3 laptops with 10 GB RAM, and mid-range Android smartphones).

---

## 1. Hardware Environment Profiles

| Constraint | Low-Spec Host Laptop | Mid-Range Android Phone |
|---|---|---|
| **CPU** | Intel Core i3-1005G1 (2 cores / 4 threads) | ARM64 Octa-Core (e.g. 2× Cortex-A76 + 6× A55) |
| **RAM** | 10 GB DDR4 Total (≈ 4.5 GB free during coding) | 6–8 GB LPDDR4X (≈ 2–3 GB free for user apps) |
| **Storage** | 192 GB NVMe SSD (≈ 127 GB free) | 128 GB UFS Internal (≈ 20–40 GB free) |
| **Cooling** | Passive / low-profile fan (throttles under sustained load) | Passive chassis dissipation (thermal throttling occurs) |

---

## 2. Resource Budgets

### 2.1 RAM Allocation Limits
- **Idle Application Overhead**: < 250 MB on Android; < 450 MB on Desktop JVM.
- **Active Agent Execution**: < 600 MB additional heap usage.
- **Gradle Android Builds**: Constrained using Gradle JVM arguments:
  `-Xmx1536m -XX:MaxMetaspaceSize=384m --max-workers=2 --no-daemon`.
- **Background Watchers & Daemon Processes**: Background daemons are strictly killed or suspended when tasks complete. No unconstrained background services are allowed to idle.

### 2.2 Storage Limits
- **Base Android APK**: < 85 MB (online flavor).
- **Offline Runtime Bundles**: Bundles are decompressed on-demand. Users can selectively install/uninstall toolchains (e.g. Flutter pack, Android SDK pack) to conserve disk space.
- **SQLite Database & Indices**: Kept below 200 MB per 10 active projects via periodic VACUUM and temporary log truncation.

### 2.3 Token & Context Budget
- **System Prompt & Skills Index**: Capped at 2,500 tokens.
- **Project Memory Summary**: Capped at 1,500 tokens.
- **Retrieved Context (RAG)**: Capped at 4,000 tokens (top-k snippets with strict truncation).
- **Rolling Conversation History**: Budgeted dynamically to reserve at least 40% of the target model's context window for output generation.

---

## 3. Risk Register & Mitigations

| Risk ID | Risk Description | Severity | Mitigation Strategy |
|---|---|---|---|
| **R-01** | **targetSdk 28 W^X Enforcement**: Android 10+ blocks executing binaries from app-private storage if targeting modern SDKs. | High | Target direct APK distribution with `targetSdk 28` via GitHub Releases / F-Droid. Document sideload instructions clearly. |
| **R-02** | **PRoot Host Leakage**: Malicious script inside PRoot accessing unencrypted app storage. | High | Encrypt all credentials in Android Keystore. Never write secrets or master tokens to PRoot filesystems. |
| **R-03** | **Phone Thermal Throttling & Battery Drain**: Heavy local builds causing high CPU temperatures and OS process termination. | Medium | Warn user before triggering local multi-module builds; pause indexing during builds; recommend desktop host for very large builds. |
| **R-04** | **Upstream Runtime Bundle Dependency**: TechJarves GitHub repository moving or removing runtime tarballs. | Medium | Mirror runtime bundle assets to MindSparQ's own repository releases during Phase 6. |
| **R-05** | **Free API Rate Limits**: Free OpenRouter and Gemini keys running out mid-task. | High | Multi-key pool with automated cooldown tracking and fallback chains ensures transparent session continuity. |

