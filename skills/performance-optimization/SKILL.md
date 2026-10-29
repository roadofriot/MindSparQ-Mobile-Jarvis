---
name: performance-optimization
description: Analyze memory allocations, rendering bottlenecks, unconstrained daemons, and low-spec CPU efficiency.
tools:
  - list_dir
  - read_file
  - search_code
---

# Performance Optimization Skill

## Purpose
Identifies resource waste, memory leaks, unconstrained daemon processes, and slow rendering on constrained low-spec hardware (i3 / 10 GB RAM laptops and mobile phones).

## Procedure
1. **Memory Budget Check**: Identify large in-memory caches, unclosed streams, or unconstrained list allocations.
2. **Process Inspection**: Ensure no unconstrained watchers, background compilers, or emulator processes run indefinitely.
3. **Lazy Initialization**: Verify heavy toolchains or database connections are initialized on demand.
4. **Rendering & Composables**: Audit UI composables for excessive recompositions and missing keys in lazy layouts.
5. **Output Recommendations**: Provide actionable optimizations to reduce RAM and CPU overhead.

