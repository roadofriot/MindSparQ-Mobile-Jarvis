# MindSparQ Mobile Jarvis — Function Ownership & Dependency Map (Phase 0)

**Date:** November 1, 2026  
**Purpose:** Precise functional decomposition map for `PocketDevApp.kt` (8,666 lines) and `MainViewModel.kt` (6,133 lines) to guarantee zero regression during Phase 1 modularization.

---

## 1. `PocketDevApp.kt` Composables Deconstruction Map

Below is the complete function-by-function inventory of all composables currently housed in `PocketDevApp.kt`, mapped to their proposed domain destination files and ViewModel dependencies:

| Line Range | Composable Function | Target Destination File | ViewModel / State Bindings | Preserved User Behavior |
| :--- | :--- | :--- | :--- | :--- |
| **313–536** | `PocketDevApp` | `ui/navigation/PocketDevApp.kt` | `viewModel.state`, `viewModel.projectsListState` | Root entry point, screen orchestration, bottom navigation |
| **537–653** | `AntigravityOnboardingScreen` | `ui/setup/AntigravityOnboardingScreen.kt` | `viewModel.startAntigravityLogin` | Google / Antigravity OAuth onboarding |
| **654–803** | `InitialLanguageSetupScreen` | `ui/setup/LanguageSetupScreen.kt` | `viewModel.setInitialLanguage` | First-run locale selector (English, Nepali, etc.) |
| **804–882** | `LanguagePickerAction` / `LanguagePickerDialog` | `ui/setup/LanguagePickerDialog.kt` | `AppLanguage` selection | Language modal switch |
| **883–1211** | `BackgroundTaskSetupScreen` / `PermissionSummaryRow` | `ui/setup/BackgroundTaskSetupScreen.kt` | `viewModel.completeBackgroundSetup` | Battery optimization & notification permission setup |
| **1212–1960** | `RuntimeSetupPromptScreen` / `DevStackChoiceRow` / `AgentChoiceRow` / `SpecRow` | `ui/setup/RuntimeSetupPromptScreen.kt` | `viewModel.installDevStack`, `viewModel.selectAgent` | Dev stack choice (Android, Python, Node) & agent selection |
| **1807–1871** | `AgentSwitchSheet` | `ui/agent/AgentSwitchSheet.kt` | `viewModel.selectAgent`, `viewModel.state.agentKind` | Quick agent switcher modal (Antigravity, Claude, DSH) |
| **1961–2526** | `RuntimeInstallationScreen` / `WorkspaceLaunchExperience` / `SetupLogPanel` / `StartupLoadingScreen` / `StartupErrorScreen` | `ui/setup/RuntimeInstallationScreen.kt` | `viewModel.state.startupStage`, `viewModel.state.setupLogs` | Terminal PRoot installation progress & diagnostic log stream |
| **2527–2736** | `RootScreenHost` | `ui/navigation/RootScreenHost.kt` | `screen` navigation state (`PROJECTS`, `AGENT`, `SETTINGS`, `INTEGRITY`) | Main screen container hosting bottom bar & active tab |
| **2737–2814** | `QuickTerminalSheet` | `ui/terminal/QuickTerminalSheet.kt` | `viewModel.runTerminalCommand`, `viewModel.clearTerminal` | Floating terminal drawer |
| **2815–3669** | `ProviderSetupScreen` / `DeviceCheckStep` / `ProviderChoiceStep` / `ProviderCredentialsStep` | `ui/setup/ProviderSetupScreen.kt` | `viewModel.updateProvider`, `viewModel.saveApiKey` | Wizard for Gemini / OpenRouter / Ollama setup |
| **3670–4578** | `ProjectsScreen` / `DashboardIconButton` / `ImportSourceButton` / `ApiStatusChip` | `ui/project/ProjectsScreen.kt` | `viewModel.projects`, `viewModel.selectProject`, `viewModel.createProject` | Project list dashboard, quick actions, status chips |
| **4706–4814** | `ProjectCard` | `ui/project/ProjectCard.kt` | `viewModel.openProject`, `viewModel.deleteProject` | Individual project cards with menu & metadata |
| **4815–4902** | `PushToGitHubDialog` | `ui/project/PushToGitHubDialog.kt` | `viewModel.pushProjectToGitHub` | Modal to create repository and push commit to GitHub |
| **4903–5143** | `MindSparQUserProfileDialog` | `ui/project/UserProfileDialog.kt` | `viewModel.githubAuthStatus`, Google CLI auth | Profile manager for Google Cloud and GitHub identity |
| **5144–5228** | `ReadOnlyProjectScreen` | `ui/project/ReadOnlyProjectScreen.kt` | Read-only inspection | Safe inspection screen for locked projects |
| **5229–5834** | `WorkspaceScreen` | `ui/workspace/WorkspaceScreen.kt` | `viewModel.activeProject`, tab switcher (`CHAT`, `FILES`, `CHANGES`, `PREVIEW`) | Full workspace ide layout with top bar and tab bar |
| **5835–5890** | `ChatSwitcherDialog` | `ui/workspace/ChatSwitcherDialog.kt` | `viewModel.switchChat`, `viewModel.newChat` | Multi-conversation switcher dialog |
| **5911–6532** | `AndroidDashboardScreen` / `ProfessionalAndroidOverview` / `AndroidHealthCheckCard` | `ui/android/AndroidDashboardScreen.kt` | `viewModel.buildAndRunAndroidApp`, `viewModel.cleanAndroidProject` | Android Gradle build controls, APK share/run, Logcat viewer |
| **6533–6857** | `FileViewerScreen` | `ui/editor/FileViewerScreen.kt` | `viewModel.openedFile`, `viewModel.saveOpenedFile` | In-app code viewer and editor with syntax highlighting |
| **6858–7231** | `FilesTab` / `FolderActionItems` | `ui/workspace/FilesTab.kt` | `viewModel.workspaceFiles`, file create/upload | Workspace file explorer tree |
| **7232–8325** | `ChatTab` / `LiveClaudeProcess` / `WorkBlockCard` / `MessageBubble` / `ApprovalCard` | `ui/agent/ChatTab.kt` | `viewModel.chatMessages`, `viewModel.sendPrompt`, `viewModel.approveTool` | Agent chat stream, thinking process cards, tool approval |
| **8369–8486** | `ChangesTab` / `DiffLineRow` | `ui/workspace/ChangesTab.kt` | `viewModel.workspaceDiff` | Interactive Git diff reviewer |
| **8487–8641** | `PreviewTab` | `ui/workspace/PreviewTab.kt` | `viewModel.previewUrl` | Web preview / loopback iframe renderer |
| **8642–8666** | `EmptyState` / `BrandMark` | `ui/components/CommonUiComponents.kt` | None | Shared UI branding & empty state placeholders |

---

## 2. `MainViewModel.kt` Scoped Delegate Decomposition Map

`MainViewModel.kt` currently holds **257 functions** across 6,133 lines. To eliminate the God-Class while maintaining backward compatibility for all UI callers, logic will be partitioned into domain delegates hosted by `MainViewModel`:

```text
MainViewModel (Orchestrator & State Container)
  │
  ├── terminalDelegate: TerminalDelegate (Lines 741–1290)
  ├── androidDelegate: AndroidBuildDelegate (Lines 1291–2250)
  ├── agentDelegate: AgentDelegate (Lines 2251–3300)
  ├── projectDelegate: ProjectDelegate (Lines 3301–4400)
  ├── workspaceDelegate: WorkspaceDelegate (Lines 4401–5150)
  └── chatDelegate: ChatDelegate (Lines 5151–6133)
```

### 2.1 Delegate Responsibility Matrix

| Delegate Class | Method Range | Core Functions | External Dependencies |
| :--- | :--- | :--- | :--- |
| **`TerminalDelegate`** | L741–L1290 | `runTerminalCommand`, `sendTerminalInput`, `interruptTerminalCommand`, `collectDeveloperDiagnostics`, `loadProjectTerminal` | `DesktopProcessRunner`, `PRoot`, `NativeSpawnProcess` |
| **`AndroidBuildDelegate`** | L1291–L2250 | `buildAndRunAndroidApp`, `cleanAndroidProject`, `cancelAndroidBuild`, `shareAndroidApk`, `startAndroidLogcat` | `AndroidBuildSupport`, `AndroidAppInstaller` |
| **`AgentDelegate`** | L2251–L3300 | `selectAgent`, `updateAgent`, `handoverTask`, `approveTool`, `syncKeyPool`, `updateProvider` | `ClaudeRuntimeBridge`, `AntigravityRuntimeBridge`, `KeyPoolManager` |
| **`ProjectDelegate`** | L3301–L4400 | `createProject`, `deleteProject`, `createTemplatedProject`, `pushProjectToGitHub`, `connectGitHubWithToken` | `ProjectStorageManager`, `GitHubClient`, `AppPreferences` |
| **`WorkspaceDelegate`** | L4401–L5150 | `readWorkspaceDirectory`, `openFile`, `saveOpenedFile`, `closeFile`, `revertFileDiff`, `detectPreviewUrl` | Filesystem IO, `WorkspaceCheckpoints` |
| **`ChatDelegate`** | L5151–L6133 | `sendPrompt`, `stopActivePrompt`, `switchChat`, `newChat`, `exportChat`, `handleVoiceResponse` | `AiGateway`, `GeminiLiveVoiceClient`, `LocalFormatGateway` |

---

## 3. End-to-End Button Action Matrix

To ensure zero dead buttons and enforce **Law 2 (Zero Fake UI or Dead Controls)**, every critical button is mapped with its explicit contract:

| Button | Screen Location | Action Handler | Preconditions | State Transition | Side Effects | Verification |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Build Android** | Android Dashboard | `buildAndRunAndroidApp()` | Project is Android, toolchain ready | `stage = BUILDING` | Forks Gradle process, streams logs, produces APK | Build exit code 0, APK generated |
| **Delete Project** | Projects Screen | `deleteProject(project)` | Project is unselected/safe | Shows confirmation dialog | Deletes directory, removes from preferences/DB | Project disappears from list |
| **Add API Key** | AI Provider Sheet | `addApiKey(provider, key, ...)` | Valid non-empty key | Key added to KeyPool | Encrypts in Keystore, verifies via API ping | Live badge: `QUOTA AVAILABLE` |
| **Switch Agent** | Quick Bar / Workspace | `selectAgent(AgentKind)` | Target runtime installed | `agentKind` updated | Updates CLI flags, starts appropriate bridge | Active agent chip updates |
| **Start Voice** | Voice Screen / Quick Bar | `startVoiceSession()` | Mic permission granted | `isListening = true` | Acquires AudioRecord, streams to Gemini/STT | Audio visualizer animates |
| **Stop Voice** | Voice Screen | `stopVoiceSession()` | Active session running | `isListening = false` | Releases AudioRecord, cleans up socket | Mic icon goes idle |
| **Run Terminal** | Terminal Sheet | `runTerminalCommand(cmd)` | Non-blank command | `running = true` | Screened by `PolicyEngine`, executed in PRoot | Output streamed to terminal buffer |
| **Push to GitHub** | Project Details | `pushProjectToGitHub(...)` | GitHub connected, repo valid | `pushing = true` | Stages files, creates commit, pushes to remote | Remote branch updated |

