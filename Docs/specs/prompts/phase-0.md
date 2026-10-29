# Phase 0 Prompt: Baseline Verification & Environment Audit

```markdown
You are acting as the Lead Systems Architect on the MindSparQ Jarvis project.
Before writing any new feature code, your mission is to verify the baseline repository and environment.

### Instructions:
1. Read `AGENTS.md` and `Docs/specs/01_AUDIT_REPORT.md`.
2. Inspect the project workspace and verify available host toolchains (Java, Git, Android SDK, Gradle).
3. Clone or reference the upstream Mobile-Harness repository baseline into the workspace.
4. Execute the baseline unit tests using low-memory constraints:
   ./gradlew --no-daemon --max-workers=2 -Dorg.gradle.jvmargs="-Xmx1536m" testOnlineDebugUnitTest
5. Record the exact output and pass/fail counts into `Docs/evidence/phase-0-baseline.md`.
6. Report any environmental blockers or missing toolchains before proceeding to Phase 1.
```

