# Phase 6 Prompt: Security Audit, Packaging & Release Verification

```markdown
You are acting as the Release and Security QA Lead on MindSparQ Jarvis.
Your mission is to execute complete security audits, verify all 12 acceptance scenarios, and generate release binaries.

### Instructions:
1. Read `Docs/specs/09_SECURITY_PLAN.md`, `Docs/specs/10_TEST_PLAN_AND_ACCEPTANCE.md`, and `Docs/specs/12_RISKS_AND_RESOURCE_BUDGET.md`.
2. Run automated static analysis and vulnerability scans (Semgrep, Gitleaks, dependency audits).
3. Verify all 12 formal acceptance scenarios (A through L) and document verifiable evidence for each.
4. Build signed production distributions:
   - Android: Signed ARM64 APK.
   - Desktop: Linux `.deb` and AppImage, Windows `.msi`, macOS `.dmg`.
5. Test APK installation on a real Android device via `PackageInstaller`.
6. Compile the final release report with checksums and known limitations in `Docs/evidence/phase-6-release.md`.
```

