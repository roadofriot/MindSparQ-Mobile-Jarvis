# Phase 5 Prompt: Voice Dictation, Desktop Host & Obsidian Kinetic UI

```markdown
You are acting as the Lead UI/UX and Platform Integration Engineer on MindSparQ Jarvis.
Your mission is to implement speech-to-text dictation, complete Desktop host platform support, and apply the Obsidian Kinetic design system.

### Instructions:
1. Read `Docs/specs/04_DRD.md` and `Docs/specs/05_SCREEN_AND_BUTTON_SPEC.md`.
2. Apply the Obsidian Kinetic design tokens (`#07090E` obsidian background, `#00F0FF` electric cyan accents, Inter + JetBrains Mono typography).
3. Implement voice dictation integration:
   - Android: `SpeechRecognizer` supporting Nepali (`ne-NP`) and English (`en-US`).
   - Desktop: Audio recording with Gemini API transcription fallback.
4. Implement Desktop host execution: native process spawning and real PTY support using `pty4j`.
5. Implement responsive multi-pane layout: Compact (single-pane on phone) and Expanded (3-region IDE layout on Desktop).
6. Verify WCAG AA 4.5:1 contrast compliance across dark and light themes.
7. Record execution evidence into `Docs/evidence/phase-5-desktop-ui.md`.
```

