# MindSparQ Harness Privacy Policy

**Effective date:** October 29, 2026

MindSparQ Harness is an open-source, local-first Android development and autonomous AI coding studio. This policy explains what information the app handles, where it is stored, and when information leaves the device.

## Information handled by the app

MindSparQ Harness handles the following information strictly under your control:

- AI-provider API keys (Google Gemini, Anthropic Claude, OpenRouter, DeepSeek, Antigravity, Ollama) and connection settings
- Prompts, AI responses, voice dictation inputs, and conversation history
- Project files, imported repositories, documents, images, audio, and code attachments
- Terminal commands, compilation output, file changes, checkpoints, and diagnostic logs
- Basic device compatibility metrics (Android version, ARM64 architecture, physical RAM governor, storage)

## Local storage & Zero Telemetry

Projects, conversations, attachments, runtime binaries, terminal history, and diagnostics are stored strictly in the app's sandboxed private storage on your device.

Provider API keys are hardware-encrypted using AES-256-GCM backed by the **Android Keystore hardware security module (KeyStore / StrongBox)** before persistence.

MindSparQ Harness contains **zero advertising SDKs, zero telemetry, zero analytics tracking, and no external user account system**. Your data never leaves your device unless explicitly routed to an AI provider of your choice.

## Information sent off the device

When you use an external AI provider (such as Google Gemini, Claude, or OpenRouter):
- The content required to fulfill your prompt (code files, terminal errors, conversation context) is transmitted directly to that provider via encrypted HTTPS.
- The provider processes that request under its own privacy policy.

When you use **On-Device Offline Models (Local SLMs)**:
- 100% of reasoning, synthesis, and code generation executes locally on your phone's CPU and RAM. **Zero data leaves the device.**

During runtime setup and maintenance, the app connects to verified software distribution services (Ubuntu rootfs, Node.js, Python, Kotlin, Android development SDKs) over HTTPS with SHA-256 checksum verification.

## Permissions

- **Internet and network state:** Download runtime components, fetch packages, and communicate with user-configured AI APIs.
- **Microphone / Speech Recognition:** Used exclusively when tapping the microphone button to dictate project names or prompts into the text fields. Audio is transcribed via Android's on-device speech engine and is never uploaded by MindSparQ.
- **Notifications:** Inform you when background builds, compilations, or agent tasks complete.
- **Foreground service & WakeLock:** Prevent Android from killing active builds and agent tasks when the screen turns off or while multitasking.
- **Document picker:** Import zip files and Git repositories you explicitly choose.
- **Install packages:** Hand off compiled APKs directly to the Android system package installer upon your confirmation.

## Retention and deletion

All local code, chats, and keys remain on your device until you delete the project or uninstall the app. Uninstalling removes all sandboxed private storage.

## Security

MindSparQ Harness implements strict Defense-in-Depth:
1. Sandboxed PRoot Linux containerization.
2. Android Keystore AES-256-GCM hardware encryption for all API keys.
3. PolicyEngine screening to guard against malicious path traversal or shell exploits.
4. MemoryGovernor preventing out-of-memory kernel panics on low-memory devices.

## Contact

For questions, issues, or contributions:
- GitHub: [https://github.com/mindsparq/MindSparQ-Mobile-Jarvis](https://github.com/mindsparq/MindSparQ-Mobile-Jarvis)
- Website: [https://mindsparq.org](https://mindsparq.org)
