# MindSparQ Mobile Jarvis — Cross-Platform Support Matrix

**Specification Reference:** `docs/ENTERPRISE_SPECIFICATION.md`  
**Core Rule:** A platform is supported only when its code path, build environment, signing, packaging, and runtime behavior have been verified.

---

## 1. Platform Support Status

| Target Platform | Tier | Architectural Status | Runtime Implementation | Build & Verification Status |
| :--- | :---: | :---: | :--- | :--- |
| **Android (Phone & Tablet)** | **Tier 1** | `IMPLEMENTED` | Native Android userland with sandboxed PRoot Linux userland and Jetpack Compose. | **Verified:** Clean assemble `app-online-debug.apk`, installed on physical Samsung Galaxy Note 10+. |
| **Linux Desktop (Ubuntu/Debian)** | **Tier 2** | `PARTIAL` | Desktop process runner implemented in `:core:runtime:DesktopProcessRunner.kt`. | **Verified:** Headless test runner passes on Ubuntu Linux 24.04 LTS host. |
| **macOS (Apple Silicon / Intel)** | **Tier 3** | `PLANNED` | KMP desktop core. Requires macOS/Xcode host for native AppKit/SwiftUI bridges. | **Blocked on Host:** Host is Ubuntu Linux. Handoff plan defined. |
| **Windows Desktop** | **Tier 3** | `PLANNED` | Win32 / WSL2 virtualization bridges. | **Deferred:** Planned for Phase 7. |
| **iOS / iPadOS** | **Tier 3** | `PLANNED` | Swift / SwiftUI wrapper over KMP core. | **Blocked on Host:** Apple platform compilation requires macOS/Xcode. |
| **Web / PWA** | **Tier 3** | `PLANNED` | WASM / CanvasKit web compilation. | **Deferred:** Secondary priority after mobile stability. |

---

## 2. Apple Platform Reality & Host Hardware Boundary

The current development environment is **Ubuntu Linux on an Intel Core i3-1005G1 CPU**. 

### Constraints:
1. **Xcode Requirement:** Apple platform compilation (iOS/macOS `.ipa`, `.app`, simulator testing, and App Store provisioning) strictly requires macOS with Xcode command-line tools.
2. **Honest Boundary Reporting:** In accordance with Non-Negotiable Rule 10, Jarvis does **not** claim iOS or macOS release readiness while running on Linux. 
3. **CI/CD Handoff Strategy:** When targeting iOS/macOS, Jarvis produces verified Kotlin Multiplatform shared code (`:core`, `:ai`, `:engine`) and exports an Xcode-ready Swift package (`JarvisKit.xcframework`) for automated compilation on a remote GitHub Actions macOS runner (`macos-14`).

