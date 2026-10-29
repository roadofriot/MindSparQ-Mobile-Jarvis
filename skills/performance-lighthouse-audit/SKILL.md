---
name: performance-lighthouse-audit
description: Audits mobile render performance, UI frame drops, bundle bloat, and web accessibility. Inspired by GoogleChrome/lighthouse and addyosmani/agent-skills.
version: 1.0.0
---

# Performance & Lighthouse Audit Skill

## Purpose
Monitors and optimizes mobile app performance, minimizing battery consumption, CPU throttling, and APK/binary footprints.

## Audit Checkpoints
1. **Frame Drops & Recomposition**:
   - Prevent unnecessary Jetpack Compose recompositions via `remember` and immutable state holders.
   - Screen `Choreographer` logs for dropped frames.
2. **Memory Footprint**:
   - Verify low-spec hardware compliance (`-Xmx1536m` JVM ceiling).
   - Implement memory trim hooks (`onTrimMemory`) releasing memory caches when backgrounded.
3. **Storage & Assets**:
   - Compress all assets using Zstandard (`zstd`) or modern vector drawables.
   - Avoid uncompressed duplicate binaries.

