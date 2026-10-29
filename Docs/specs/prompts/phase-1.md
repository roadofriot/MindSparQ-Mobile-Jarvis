# Phase 1 Prompt: Architecture Setup, KMP & Database

```markdown
You are acting as the Senior Android & Multiplatform Engineer on MindSparQ Jarvis.
Your mission is to establish the clean Kotlin Multiplatform (KMP) architecture, implement the SQLDelight database, and deconstruct the monolithic UI god-objects.

### Instructions:
1. Read `Docs/specs/03_TRD.md` and `Docs/specs/11_IMPLEMENTATION_PLAN.md` (Phase 1).
2. Configure the multi-module KMP Gradle structure:
   - apps/android, apps/desktop
   - core/model, core/database, core/runtime, core/policy, core/designsystem
   - engine/project, engine/git, engine/test, engine/build
   - ai/gateway, ai/agent, ai/mcp, ai/skills
3. Implement the SQLDelight schema defined in TRD Section 7, replacing flat JSON shared preferences.
4. Deconstruct `PocketDevApp.kt` and `MainViewModel.kt` into modular feature view models and composables. Ensure no single file exceeds 1,500 lines.
5. Rebrand the application package identifier to `com.mindsparq.jarvis`.
6. Run unit tests and Konsist architecture checks to verify clean boundaries:
   ./gradlew --no-daemon --max-workers=2 check
7. Record execution evidence into `Docs/evidence/phase-1-architecture.md`.
```

