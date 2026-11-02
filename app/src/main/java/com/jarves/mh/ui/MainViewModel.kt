package com.jarves.mh.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarves.mh.BuildConfig
import com.jarves.mh.R
import com.jarves.mh.data.ApiKeyInfo
import com.jarves.mh.data.ApiKeyVault
import com.jarves.mh.data.AppPreferences
import com.jarves.mh.model.ActivityItem
import com.jarves.mh.model.AgentKind
import com.jarves.mh.model.AndroidTemplate
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.ChatAttachment
import com.jarves.mh.model.ChatMessage
import com.jarves.mh.model.DevStack
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.ProviderKind
import com.jarves.mh.model.ProviderProfile
import com.jarves.mh.model.RuntimeEvent
import com.jarves.mh.model.ToolRequest
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.model.resolveFailoverChain
import com.jarves.mh.network.ConnectionValidation
import com.jarves.mh.network.DiscoveredModel
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.network.ModelDiscoveryResult
import com.jarves.mh.network.ProviderApiClient
import com.jarves.mh.runtime.AgentRegistry
import com.jarves.mh.runtime.AgentUpdateInfo
import com.jarves.mh.runtime.AndroidAction
import com.jarves.mh.runtime.AndroidApkInfo
import com.jarves.mh.runtime.AndroidAppInstaller
import com.jarves.mh.runtime.AndroidBuildIssue
import com.jarves.mh.runtime.AndroidBuildPhase
import com.jarves.mh.runtime.AndroidBuildRecord
import com.jarves.mh.runtime.AndroidBuildStage
import com.jarves.mh.runtime.AndroidHealthCheck
import com.jarves.mh.runtime.AndroidHealthFix
import com.jarves.mh.runtime.AndroidLogLevel
import com.jarves.mh.runtime.AndroidLogLine
import com.jarves.mh.runtime.AndroidLogcatState
import com.jarves.mh.runtime.AntigravityAuthController
import com.jarves.mh.runtime.AntigravityAuthState
import com.jarves.mh.runtime.AntigravityAuthStatus
import com.jarves.mh.runtime.AntigravityRuntimeBridge
import com.jarves.mh.runtime.ClaudeRuntimeBridge
import com.jarves.mh.runtime.DshRuntimeBridge
import com.jarves.mh.runtime.IntegrityScheduler
import com.jarves.mh.runtime.RuntimeInstaller
import com.jarves.mh.runtime.RuntimeSetupController
import com.jarves.mh.runtime.SupportedProjectTemplate
import com.jarves.mh.runtime.WorkspaceCheckpoints
import com.jarves.mh.runtime.localServerPlaceholderKey
import com.jarves.mh.ui.delegate.AgentDelegate
import com.jarves.mh.ui.delegate.AndroidBuildDelegate
import com.jarves.mh.ui.delegate.ChatDelegate
import com.jarves.mh.ui.delegate.ProjectDelegate
import com.jarves.mh.ui.delegate.ProjectGitDelegate
import com.jarves.mh.ui.delegate.TerminalDelegate
import com.jarves.mh.ui.delegate.WorkspaceDelegate
import com.jarves.mh.update.AppUpdateInfo
import com.jarves.mh.update.AppUpdater
import com.mindsparq.jarvis.ai.gateway.KeyPoolManager
import com.mindsparq.jarvis.ai.gateway.ManagedKey
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    internal fun str(@StringRes id: Int, vararg args: Any): String = AppStrings.get(getApplication(), id, *args)
    internal fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        AppStrings.context(getApplication()).resources.getQuantityString(id, count, *args)

    internal val vault = ApiKeyVault(application)
    internal val preferences = AppPreferences(application)
    internal val keyPoolManager = KeyPoolManager()
    internal val workspaceCheckpoints = WorkspaceCheckpoints(application.filesDir, application)
    internal val integrityScheduler = IntegrityScheduler(application, workspaceCheckpoints)

    internal fun syncKeyPool() {
        ProviderKind.entries.forEach { kind ->
            val credentials = vault.credentials(kind.name)
            val managed = credentials.mapIndexed { index, cred ->
                ManagedKey(
                    id = cred.id,
                    providerId = kind.name,
                    secretKey = cred.secret,
                    label = cred.name,
                    accountLabel = cred.accountLabel,
                    quotaScopeId = cred.quotaScopeId,
                    billingTier = if (cred.billingTier.equals("PAID", ignoreCase = true)) com.mindsparq.jarvis.core.model.BillingTier.PAID else com.mindsparq.jarvis.core.model.BillingTier.FREE,
                    priority = if (cred.isActive) 1 else cred.priority,
                    state = when (cred.state) {
                        "COOLING" -> com.mindsparq.jarvis.ai.gateway.KeyState.COOLING
                        "EXHAUSTED" -> com.mindsparq.jarvis.ai.gateway.KeyState.EXHAUSTED
                        "INVALID" -> com.mindsparq.jarvis.ai.gateway.KeyState.INVALID
                        else -> com.mindsparq.jarvis.ai.gateway.KeyState.HEALTHY
                    }
                )
            }
            keyPoolManager.registerKeys(kind.name, managed)
        }
    }

    internal val claudeRuntime = ClaudeRuntimeBridge(
        context = application,
        secretFor = { profile: ProviderProfile ->
            val saved = vault.get(profile.kind.name).orEmpty()
            if (saved.isNotBlank()) {
                saved
            } else {
                val placeholder = localServerPlaceholderKey(profile)
                if (placeholder.isNotBlank()) placeholder else null
            }
        },
        keyPoolManager = keyPoolManager,
        failoverProfiles = {
            val chain = resolveFailoverChain(_state.value.provider.failoverChain)
            chain.map { kind ->
                ProviderProfile(
                    kind = kind,
                    baseUrl = kind.defaultBaseUrl,
                    model = kind.defaultModel,
                )
            }
        }
    )

    internal val dshRuntime = DshRuntimeBridge(
        context = application,
        keyPoolManager = keyPoolManager,
    ) { profile: ProviderProfile ->
        val saved = vault.get(profile.kind.name).orEmpty()
        if (saved.isNotBlank()) {
            saved
        } else {
            val placeholder = localServerPlaceholderKey(profile)
            if (placeholder.isNotBlank()) placeholder else null
        }
    }

    internal val installer = RuntimeInstaller(application)

    internal val antigravityRuntime = AntigravityRuntimeBridge(
        application,
        model = { _state.value.antigravityModel },
        effort = { _state.value.antigravityEffort },
        conversationId = { projectId ->
            _state.value.activeChatId?.let { preferences.loadAgentConversation(AgentKind.ANTIGRAVITY, projectId, it) }
        },
        saveConversationId = { projectId, id ->
            _state.value.activeChatId?.let { preferences.saveAgentConversation(AgentKind.ANTIGRAVITY, projectId, it, id) }
        },
    )

    internal val agentRegistry = AgentRegistry.builtIns(claudeRuntime, dshRuntime, antigravityRuntime)
    internal fun activeRuntime(): com.jarves.mh.runtime.RuntimeBridge = agentRegistry.require(_state.value.agentKind).runtime
    internal val providerApi = ProviderApiClient(application)
    internal fun appUpdater(): AppUpdater = AppUpdater(
        getApplication(),
        if (BuildConfig.DEBUG) preferences.debugUpdateManifestUrl else "",
    )

    internal val offlineModelManager = com.mindsparq.jarvis.ai.gateway.OfflineModelManager(
        baseStorageDir = getApplication<Application>().filesDir
    )

    private var lastOpenedAntigravityAuthUrl: String? = null

    internal val antigravityAuthController = AntigravityAuthController(
        application,
        preferences.antigravitySignedIn,
        preferences.antigravityAccountEmail,
    ) { signedIn, email ->
        preferences.antigravitySignedIn = signedIn
        preferences.antigravityAccountEmail = email.orEmpty()
        if (!signedIn) preferences.clearAgentConversations(AgentKind.ANTIGRAVITY)
    }

    private val initialAgentKind = AgentKind.fromStored(preferences.agentKind).let {
        if (it == AgentKind.CLAUDE_CODE || it == AgentKind.ANTIGRAVITY || it == AgentKind.DEEPSEEK_HARNESS) AgentKind.OPENCODE else it
    }
    private val initialPrimaryAgentKind = preferences.primaryAgentKind
        .takeIf(String::isNotBlank)
        ?.let(AgentKind::fromStored)?.let {
            if (it == AgentKind.CLAUDE_CODE || it == AgentKind.ANTIGRAVITY || it == AgentKind.DEEPSEEK_HARNESS) AgentKind.OPENCODE else it
        }
        ?: initialAgentKind

    internal val _state = MutableStateFlow(
        AppUiState(
            startupMessage = str(R.string.vm_checking_device),
            onboardingComplete = preferences.onboardingComplete,
            backgroundSetupComplete = preferences.backgroundSetupComplete,
            initialLanguageSelected = preferences.initialLanguageSelected,
            agentKind = initialAgentKind,
            primaryAgentKind = initialPrimaryAgentKind,
            provider = preferences.loadProvider(vault, initialAgentKind),
            activeApiKeyName = vault.list(preferences.loadProvider(vault, initialAgentKind).kind.name)
                .firstOrNull(ApiKeyInfo::isActive)?.name,
            antigravityAuth = AntigravityAuthState(
                status = if (preferences.antigravitySignedIn) AntigravityAuthStatus.SIGNED_IN else AntigravityAuthStatus.SIGNED_OUT,
                message = preferences.antigravityAccountEmail.takeIf(String::isNotBlank)?.let { str(R.string.vm_connected_as, it) },
                accountEmail = preferences.antigravityAccountEmail.takeIf(String::isNotBlank),
            ),
            antigravityModel = preferences.antigravityModel,
            antigravityEffort = preferences.antigravityEffort,
            themeMode = runCatching { com.jarves.mh.ui.theme.AppThemeMode.valueOf(preferences.themeMode.uppercase()) }
                .getOrDefault(com.jarves.mh.ui.theme.AppThemeMode.DARK),
            languageCode = preferences.languageCode,
            projects = preferences.loadProjects(),
            githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
            githubLogin = preferences.githubLogin.takeIf(String::isNotBlank),
            selectedDevStacks = preferences.selectedDevStacks.mapNotNull { name ->
                runCatching { DevStack.valueOf(name) }.getOrNull()
            }.toSet() + DevStack.WEB,
        ),
    )

    val state: StateFlow<AppUiState> = _state.asStateFlow()

    internal fun projectGuestRoot(project: Project): String = "/workspace/${project.slug}"

    internal fun projectWorkspaceRoot(project: Project): File {
        val base = File(getApplication<Application>().filesDir, "workspaces/${project.id}")
            .apply { mkdirs() }
            .canonicalFile
        if (project.rootPath.isBlank()) return base
        val selected = File(base, project.rootPath).canonicalFile
        require(selected.toPath().startsWith(base.toPath())) { str(R.string.vm_unsafe_project_root) }
        return selected.apply { mkdirs() }
    }

    val chatDelegate: ChatDelegate = ChatDelegate(this)
    val terminalDelegate: TerminalDelegate = TerminalDelegate(this)
    val androidDelegate: AndroidBuildDelegate = AndroidBuildDelegate(this)
    val agentDelegate: AgentDelegate = AgentDelegate(this)
    val projectDelegate: ProjectDelegate = ProjectDelegate(this)
    val gitDelegate: ProjectGitDelegate = ProjectGitDelegate(this)
    val workspaceDelegate: WorkspaceDelegate = WorkspaceDelegate(this)

    init {
        syncKeyPool()
        agentDelegate.autoUpdateFreeModelsDaily()
        viewModelScope.launch {
            AndroidAppInstaller.events.collect { event ->
                if (event.operationId != androidDelegate.pendingAndroidInstallId) return@collect
                val project = _state.value.activeProject
                val launchRequested = androidDelegate.pendingAndroidInstallLaunch
                androidDelegate.pendingAndroidInstallId = null
                androidDelegate.pendingAndroidInstallLaunch = false
                if (event.success) {
                    if (event.launched) androidDelegate.androidLaunchAtMillis = System.currentTimeMillis()
                    val message = when {
                        event.launched -> str(R.string.an_app_installed_launched)
                        launchRequested -> str(R.string.an_app_installed_no_launcher)
                        else -> str(R.string.an_apk_installed)
                    }
                    _state.update {
                        it.copy(
                            androidBuildRunning = false,
                            androidBuildPhase = AndroidBuildPhase.SUCCEEDED,
                            androidBuildStage = AndroidBuildStage.COMPLETE,
                            androidBuildMessage = message,
                            androidBuildFinishedAtMillis = System.currentTimeMillis(),
                            toastMessage = message,
                        )
                    }
                    project?.let { androidDelegate.saveAndroidBuildRecord(it.id) }
                    androidDelegate.refreshAndroidApkInfo()
                } else {
                    val message = event.message ?: str(R.string.an_apk_install_failed)
                    _state.update {
                        it.copy(
                            androidBuildRunning = false,
                            androidBuildPhase = AndroidBuildPhase.FAILED,
                            androidBuildStage = AndroidBuildStage.FAILED,
                            androidBuildMessage = message,
                            androidBuildIssues = listOf(AndroidBuildIssue(title = str(R.string.rt_apk_install_failed), detail = message)),
                            androidBuildFinishedAtMillis = System.currentTimeMillis(),
                            toastMessage = message,
                        )
                    }
                    project?.let { androidDelegate.saveAndroidBuildRecord(it.id) }
                }
            }
        }
        vault.remove(LEGACY_GITHUB_TOKEN_KEY)
        viewModelScope.launch { projectDelegate.refreshGitHubConnection() }
        RuntimeSetupController.restore(application)
        viewModelScope.launch { dshRuntime.events.collect { event -> chatDelegate.onRuntimeEvent(event) } }
        viewModelScope.launch { antigravityRuntime.events.collect { event -> chatDelegate.onRuntimeEvent(event) } }
        viewModelScope.launch { claudeRuntime.events.collect { event -> chatDelegate.onRuntimeEvent(event) } }
        viewModelScope.launch { RuntimeSetupController.snapshot.collect { snapshot -> agentDelegate.onSetupSnapshot(snapshot) } }
        viewModelScope.launch { agentDelegate.bootstrap() }
        viewModelScope.launch {
            antigravityAuthController.state.collect { auth ->
                _state.update { it.copy(antigravityAuth = auth) }
                auth.authorizationUrl?.takeIf { it != lastOpenedAntigravityAuthUrl }?.let { url ->
                    lastOpenedAntigravityAuthUrl = url
                    runCatching {
                        getApplication<Application>().startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }.onFailure {
                        _state.update { state -> state.copy(toastMessage = str(R.string.vm_browser_open_failed_signin)) }
                    }
                }
            }
        }
        if (antigravityAuthController.hasOfficialCredential() &&
            (!preferences.antigravitySignedIn || preferences.antigravityAccountEmail.isBlank())
        ) {
            viewModelScope.launch { antigravityAuthController.beginLogin() }
        }
        if (!preferences.legacySeededCredentialRemoved) {
            vault.remove(ProviderKind.CUSTOM.name)
            preferences.legacySeededCredentialRemoved = true
            _state.update { current ->
                if (current.provider.kind == ProviderKind.CUSTOM) {
                    current.copy(provider = current.provider.copy(hasSecret = false))
                } else current
            }
        }
        if (
            BuildConfig.TEST_OPENROUTER_API_KEY.isNotBlank() &&
            preferences.testProviderDefaultsVersion < TEST_PROVIDER_DEFAULTS_VERSION
        ) {
            val testProvider = ProviderProfile(
                kind = ProviderKind.CUSTOM,
                baseUrl = TEST_OPENROUTER_BASE_URL,
                model = TEST_OPENROUTER_MODEL,
                hasSecret = true,
            )
            vault.put(ProviderKind.CUSTOM.name, BuildConfig.TEST_OPENROUTER_API_KEY)
            preferences.saveProvider(testProvider, _state.value.agentKind)
            preferences.testProviderDefaultsVersion = TEST_PROVIDER_DEFAULTS_VERSION
            _state.update { it.copy(provider = testProvider) }
        }

        val loadedProjects = preferences.loadProjects()
        val cleanedProjects = loadedProjects.filter { project ->
            if (project.kind == ProjectKind.QUICK_PROJECT) {
                val chats = preferences.loadProjectChats(project.id)
                val userMessages = chats.sumOf { preferences.loadMessages(project.id, it.id).count { m -> m.fromUser } }
                val workspaceDir = File(application.filesDir, "workspaces/${project.id}")
                val userFiles = if (workspaceDir.isDirectory) {
                    workspaceDir.walkTopDown().filter { file ->
                        file.isFile && !file.name.startsWith(".claude") && file.name != ".pocket-dev-stacks.json"
                    }.count()
                } else 0
                val keep = userMessages > 0 || userFiles > 0
                if (!keep) {
                    workspaceDir.deleteRecursively()
                    terminalDelegate.terminalHistoryFile(project.id).delete()
                    preferences.deleteProjectChats(project.id)
                }
                keep
            } else true
        }
        if (cleanedProjects.size != loadedProjects.size) {
            preferences.saveProjects(cleanedProjects)
            _state.update { it.copy(projects = cleanedProjects) }
        }
    }

    // --- Terminal Forwarders ---
    val terminalLines: StateFlow<List<TerminalOutputLine>> get() = terminalDelegate.terminalLines
    val isTerminalRunning: StateFlow<Boolean> get() = terminalDelegate.isTerminalRunning
    val terminalLiveOutput: StateFlow<String> get() = terminalDelegate.terminalLiveOutput
    val terminalCurrentCommand: StateFlow<String?> get() = terminalDelegate.terminalCurrentCommand
    fun runTerminalCommand(cmd: String) = terminalDelegate.runTerminalCommand(cmd)
    fun sendTerminalInput(text: String) = terminalDelegate.sendTerminalInput(text)
    fun interruptTerminalCommand() = terminalDelegate.interruptTerminalCommand()
    fun clearTerminal() = terminalDelegate.clearTerminal()
    fun requestProjectTerminalCommand(command: String) = terminalDelegate.requestProjectTerminalCommand(command)
    fun prepareProjectTerminalCommand(command: String) = terminalDelegate.prepareProjectTerminalCommand(command)
    fun consumeProjectTerminalDraft() = terminalDelegate.consumeProjectTerminalDraft()
    fun openProjectTerminal() = terminalDelegate.openProjectTerminal()
    fun confirmProjectTerminalCommand() = terminalDelegate.confirmProjectTerminalCommand()
    fun cancelProjectTerminalCommand() = terminalDelegate.cancelProjectTerminalCommand()
    fun stopProjectTerminalCommand() = terminalDelegate.stopProjectTerminalCommand()
    fun sendProjectTerminalInput(text: String) = terminalDelegate.sendProjectTerminalInput(text)
    fun interruptProjectTerminalCommand() = terminalDelegate.interruptProjectTerminalCommand()
    fun clearProjectTerminal() = terminalDelegate.clearProjectTerminal()
    fun refreshDeveloperDiagnostics() = terminalDelegate.refreshDeveloperDiagnostics()
    fun stopInactiveDeveloperProcesses() = terminalDelegate.stopInactiveDeveloperProcesses()
    fun clearDeveloperRuntimeCache() = terminalDelegate.clearDeveloperRuntimeCache()
    fun releaseMemoryCaches() = terminalDelegate.releaseMemoryCaches()

    // --- Android Forwarders ---
    fun refreshAndroidDashboard() = androidDelegate.refreshAndroidDashboard()
    fun saveAndroidApk(uri: Uri) = androidDelegate.saveAndroidApk(uri)
    fun openInstalledAndroidAppDetails() = androidDelegate.openInstalledAndroidAppDetails()
    fun uninstallAndroidApp() = androidDelegate.uninstallAndroidApp()
    fun performAndroidHealthFix(fix: AndroidHealthFix) = androidDelegate.performAndroidHealthFix(fix)

    fun buildAndRunAndroidApp() = androidDelegate.buildAndRunAndroidApp()
    fun performAndroidAction(action: AndroidAction) = androidDelegate.performAndroidAction(action)
    fun cleanAndroidProject() = androidDelegate.cleanAndroidProject()
    fun cancelAndroidBuild() = androidDelegate.cancelAndroidBuild()
    fun shareAndroidApk() = androidDelegate.shareAndroidApk()
    fun startAndroidHealthCheck() = androidDelegate.refreshAndroidHealth()
    fun refreshAndroidHealth() = androidDelegate.refreshAndroidHealth()
    fun applyAndroidHealthFix(fix: AndroidHealthFix) = androidDelegate.applyAndroidHealthFix(fix)
    fun openAndroidIssue(issue: AndroidBuildIssue) = androidDelegate.openAndroidIssue(issue)
    fun startAndroidLogcat() = androidDelegate.startAndroidLogcat()
    fun stopAndroidLogcat() = androidDelegate.stopAndroidLogcat()
    fun toggleAndroidLogcatPause() = androidDelegate.toggleAndroidLogcatPause()
    fun clearAndroidLogcat() = androidDelegate.clearAndroidLogcat()
    fun setAndroidLogcatQuery(value: String) = androidDelegate.setAndroidLogcatQuery(value)
    fun setAndroidLogcatLevel(value: AndroidLogLevel) = androidDelegate.setAndroidLogcatLevel(value)
    fun openAndroidLogLine(line: AndroidLogLine) = androidDelegate.openAndroidLogLine(line)
    internal fun saveAndroidBuildRecord(projectId: String) = androidDelegate.saveAndroidBuildRecord(projectId)
    internal fun loadAndroidBuildRecord(projectId: String) = androidDelegate.loadAndroidBuildRecord(projectId)
    internal fun refreshAndroidApkInfo() = androidDelegate.refreshAndroidApkInfo()

    // --- Agent & Settings Forwarders ---
    fun toggleTheme() = agentDelegate.toggleTheme()
    fun setThemeMode(mode: com.jarves.mh.ui.theme.AppThemeMode) = agentDelegate.setThemeMode(mode)
    fun setLanguage(code: String) = agentDelegate.setLanguage(code)
    fun finishInitialLanguageSetup() = agentDelegate.finishInitialLanguageSetup()
    fun reopenInitialLanguageSetup() = agentDelegate.reopenInitialLanguageSetup()
    fun getSavedApiKey(kind: ProviderKind): String = agentDelegate.getSavedApiKey(kind)
    fun saveApiKey(kind: ProviderKind, secret: String) = agentDelegate.saveApiKey(kind, secret)
    fun saveGeminiApiKey(secret: String) = agentDelegate.saveGeminiApiKey(secret)
    fun getSavedApiKeys(kind: ProviderKind) = agentDelegate.getSavedApiKeys(kind)
    fun addApiKey(kind: ProviderKind, name: String, secret: String): List<ApiKeyInfo> =
        agentDelegate.addApiKey(kind, name, secret)
    fun addApiKey(
        kind: ProviderKind,
        name: String,
        secret: String,
        accountLabel: String = "Default Account",
        quotaScopeId: String = "default-project",
        billingTier: String = "FREE",
        priority: Int = 10,
    ): List<ApiKeyInfo> = agentDelegate.addApiKey(kind, name, secret, accountLabel, quotaScopeId, billingTier, priority)
    fun activateApiKey(kind: ProviderKind, keyId: String): List<ApiKeyInfo> = agentDelegate.activateApiKey(kind, keyId)
    fun removeApiKey(kind: ProviderKind, keyId: String): List<ApiKeyInfo> = agentDelegate.removeApiKey(kind, keyId)
    fun getFailoverChain() = agentDelegate.getFailoverChain()
    fun isFailoverParallel() = agentDelegate.isFailoverParallel()
    fun saveFailoverChain(providers: List<ProviderKind>) = agentDelegate.saveFailoverChain(providers)
    fun toggleFailoverParallel() = agentDelegate.toggleFailoverParallel()
    fun downloadOfflineModel(modelId: String, onProgress: (Float) -> Unit, onComplete: (Boolean) -> Unit) =
        agentDelegate.downloadOfflineModel(modelId, onProgress, onComplete)
    fun deleteOfflineModel(modelId: String) = agentDelegate.deleteOfflineModel(modelId)
    fun startRuntimeSetup() = agentDelegate.startRuntimeSetup()
    fun retryStartup() = agentDelegate.retryStartup()
    fun finishOnboarding(profile: ProviderProfile, secret: String) = agentDelegate.finishOnboarding(profile, secret)
    fun finishAntigravityOnboarding() = agentDelegate.finishAntigravityOnboarding()
    fun chooseOnboardingAgent(kind: AgentKind) = agentDelegate.chooseOnboardingAgent(kind)
    fun updateProvider(profile: ProviderProfile, secret: String) = agentDelegate.updateProvider(profile, secret)
    fun finishBackgroundSetup() = agentDelegate.finishBackgroundSetup()
    fun selectAgent(kind: AgentKind) = agentDelegate.selectAgent(kind)
    fun installAgent(kind: AgentKind) = agentDelegate.installAgent(kind)
    fun checkAgentUpdates() = agentDelegate.checkAgentUpdates()
    fun updateAgent(kind: AgentKind) = agentDelegate.updateAgent(kind)
    fun startAntigravityLogin() = agentDelegate.startAntigravityLogin()
    fun submitAntigravityCode(code: String) = agentDelegate.submitAntigravityCode(code)
    fun logoutAntigravity() = agentDelegate.logoutAntigravity()
    fun setAntigravityModel(model: String) = agentDelegate.setAntigravityModel(model)
    fun setAntigravityEffort(effort: String) = agentDelegate.setAntigravityEffort(effort)
    fun refreshAntigravityModels() = agentDelegate.refreshAntigravityModels()
    fun toggleDevStack(stack: DevStack) = agentDelegate.toggleDevStack(stack)
    fun installDevStack(stack: DevStack, refreshAndroidHealthAfter: Boolean = false) =
        agentDelegate.installDevStack(stack, refreshAndroidHealthAfter)
    fun removeDevStack(stack: DevStack) = agentDelegate.removeDevStack(stack)
    fun getCachedModels(kind: ProviderKind) = agentDelegate.getCachedModels(kind)
    suspend fun discoverModels(profile: ProviderProfile, secret: String) = agentDelegate.discoverModels(profile, secret)
    suspend fun validateProvider(
        profile: ProviderProfile,
        secret: String,
        models: List<DiscoveredModel>,
    ): ConnectionValidation = agentDelegate.validateProvider(profile, secret, models)
    suspend fun discoverModelsMulti(parallel: Boolean = false) = agentDelegate.discoverModelsMulti(parallel)
    suspend fun validateMulti(model: String, parallel: Boolean = false) = agentDelegate.validateMulti(model, parallel)
    fun pingApi() = agentDelegate.pingApi()
    fun testAntigravityConnection() = agentDelegate.testAntigravityConnection()
    fun clearPingStatus() = agentDelegate.clearPingStatus()
    fun switchToFreeBackup() = agentDelegate.switchToFreeBackup()
    fun switchToOfflineMode() = agentDelegate.switchToOfflineMode()
    fun startOnlineAutoUpdateWatcher() = agentDelegate.startOnlineAutoUpdateWatcher()
    fun checkForAppUpdate(force: Boolean = false) = agentDelegate.checkForAppUpdate(force)
    fun setDebugUpdateManifestUrl(url: String) = agentDelegate.setDebugUpdateManifestUrl(url)
    fun clearDebugUpdateManifestUrl() = agentDelegate.clearDebugUpdateManifestUrl()
    fun debugUpdateManifestUrl(): String = agentDelegate.debugUpdateManifestUrl()
    fun installAppUpdate() = agentDelegate.installAppUpdate()
    fun dismissAppUpdateError() = agentDelegate.dismissAppUpdateError()

    // --- Autonomous State Machine Forwarders (Phase 6) ---
    fun resumeOrchestratorForProject(projectId: String) = chatDelegate.resumeOrchestratorForProject(projectId)
    fun getActiveTaskSnapshot() = chatDelegate.getActiveTaskSnapshot()

    // --- Project Forwarders ---
    fun createQuickProject() = projectDelegate.createQuickProject()
    fun clonePublicGitRepository(url: String) = projectDelegate.clonePublicGitRepository(url)
    fun generateNewGitHubCode() = projectDelegate.generateNewGitHubCode()
    fun refreshGitHubRepositories() = projectDelegate.refreshGitHubRepositories()
    fun renameProject(projectId: String, newName: String) = projectDelegate.renameProject(projectId, newName)

    fun openProject(project: Project) = projectDelegate.openProject(project)
    fun closeProject() = projectDelegate.closeProject()
    fun closeReadOnlyProject() = projectDelegate.closeReadOnlyProject()
    fun switchReadOnlyChat(chatId: String) = projectDelegate.switchReadOnlyChat(chatId)
    fun activateReadOnlyProject() = projectDelegate.activateReadOnlyProject()
    fun consumeToast() = projectDelegate.consumeToast()
    fun createProject(name: String) = projectDelegate.createProject(name)
    fun openProjectInFileManager(projectName: String) = projectDelegate.openProjectInFileManager(projectName)
    fun scaffoldVoiceArchitectProject(
        name: String,
        template: AndroidTemplate?,
        prd: String,
        trd: String,
        architecture: String,
        decisions: String = "",
    ) = projectDelegate.scaffoldVoiceArchitectProject(name, template, prd, trd, architecture, decisions)
    fun createAndroidProject(name: String, template: AndroidTemplate) =
        projectDelegate.createAndroidProject(name, template)
    fun createTemplatedProject(name: String, template: SupportedProjectTemplate) =
        projectDelegate.createTemplatedProject(name, template)
    fun deleteProject(projectId: String) = projectDelegate.deleteProject(projectId)
    fun deleteProject(project: Project) = projectDelegate.deleteProject(project.id)
    fun pushProjectToGitHub(
        project: Project,
        repoName: String,
        isPrivate: Boolean,
        onResult: (Boolean, String) -> Unit = { _, _ -> },
    ) = projectDelegate.pushProjectToGitHub(project, repoName, isPrivate, onResult)
    fun startGitHubLogin() = projectDelegate.startGitHubLogin()
    fun disconnectGitHub() = projectDelegate.disconnectGitHub()
    fun connectGitHubWithToken(token: String) = projectDelegate.connectGitHubWithToken(token)
    suspend fun refreshGitHubConnection() = projectDelegate.refreshGitHubConnection()
    fun cloneGitHubRepository(repository: GitHubRepository) = projectDelegate.cloneGitHubRepository(repository)
    fun importZipProject(uri: Uri) = projectDelegate.importZipProject(uri)
    fun exportActiveProject(uri: Uri, selection: ExportSelection) = projectDelegate.exportActiveProject(uri, selection)
    fun useSuggestedProjectRoot() = projectDelegate.useSuggestedProjectRoot()
    fun integrityResultFor(projectId: String) = projectDelegate.integrityResultFor(projectId)
    fun allIntegrityResults() = projectDelegate.allIntegrityResults()
    fun integrityCheckIntervalMinutes() = projectDelegate.integrityCheckIntervalMinutes()
    fun setIntegrityCheckIntervalMinutes(minutes: Long) = projectDelegate.setIntegrityCheckIntervalMinutes(minutes)
    suspend fun runIntegrityCheck(projectId: String, projectName: String, workspaceDir: File) =
        projectDelegate.runIntegrityCheck(projectId, projectName, workspaceDir)
    suspend fun runIntegrityCheckAll() = projectDelegate.runIntegrityCheckAll()
    fun resetIntegrityBaseline(projectId: String) = projectDelegate.resetIntegrityBaseline(projectId)

    // --- Workspace Forwarders ---
    fun openFile(entry: WorkspaceEntry) = workspaceDelegate.openFile(entry)
    fun loadOpenedFile(project: Project, path: String, targetLine: Int? = null, targetColumn: Int? = null) =
        workspaceDelegate.loadOpenedFile(project, path, targetLine, targetColumn)
    fun closeFile() = workspaceDelegate.closeFile()
    fun updateFileDraft(value: String) = workspaceDelegate.updateFileDraft(value)
    fun beginFileEdit() = workspaceDelegate.beginFileEdit()
    fun cancelFileEdit() = workspaceDelegate.cancelFileEdit()
    fun reloadOpenedFile() = workspaceDelegate.reloadOpenedFile()
    fun dismissFileConflict() = workspaceDelegate.dismissFileConflict()
    fun saveOpenedFile() = workspaceDelegate.saveOpenedFile()
    fun refreshProjectFiles() = workspaceDelegate.refreshProjectFiles()
    fun loadProjectDirectory(relativePath: String) = workspaceDelegate.loadProjectDirectory(relativePath)
    fun createWorkspaceEntry(parentPath: String, name: String, isDirectory: Boolean) =
        workspaceDelegate.createWorkspaceEntry(parentPath, name, isDirectory)
    fun renameWorkspaceEntry(path: String, newName: String) = workspaceDelegate.renameWorkspaceEntry(path, newName)
    fun deleteWorkspaceEntry(path: String) = workspaceDelegate.deleteWorkspaceEntry(path)
    fun uploadToWorkspace(parentPath: String, uris: List<Uri>) = workspaceDelegate.uploadToWorkspace(parentPath, uris)
    fun shareWorkspaceFile(path: String) = workspaceDelegate.shareWorkspaceFile(path)
    fun saveWorkspaceFileCopy(path: String, destination: Uri) = workspaceDelegate.saveWorkspaceFileCopy(path, destination)

    // --- Chat Forwarders ---
    fun createChat() = chatDelegate.createChat()
    fun switchChat(chatId: String) = chatDelegate.switchChat(chatId)
    fun addChatAttachments(uris: List<Uri>) = chatDelegate.addChatAttachments(uris)
    fun removePendingAttachment(attachmentId: String) = chatDelegate.removePendingAttachment(attachmentId)
    fun openChatAttachment(attachment: ChatAttachment) = chatDelegate.openChatAttachment(attachment)
    fun sendPrompt(prompt: String) = chatDelegate.sendPrompt(prompt)
    fun steerQueuedFollowUp(id: String) = chatDelegate.steerQueuedFollowUp(id)
    fun removeQueuedFollowUp(id: String) = chatDelegate.removeQueuedFollowUp(id)
    fun isDeviceOnline(): Boolean = chatDelegate.isDeviceOnline()
    fun answerApproval(approved: Boolean) = chatDelegate.answerApproval(approved)
    fun stopTask() = chatDelegate.stopTask()
    fun undoLastChanges() = chatDelegate.undoLastChanges()
    fun keepLastChanges() = chatDelegate.keepLastChanges()
    fun undoFileChange(path: String) = chatDelegate.undoFileChange(path)
    fun keepFileChange(path: String) = chatDelegate.keepFileChange(path)
    fun handoverTask() = chatDelegate.handoverTask()
    fun dismissHandover() = chatDelegate.dismissHandover()
    internal fun persistMessages(includeLiveProcess: Boolean = true) = chatDelegate.persistMessages(includeLiveProcess)
    internal fun onRuntimeEvent(event: RuntimeEvent) = chatDelegate.onRuntimeEvent(event)

    override fun onCleared() {
        stopAndroidLogcat()
        super.onCleared()
    }

    companion object {
        const val MAX_VISIBLE_WORK_ITEMS = 32
        val DEFERRED_FOLDER_METADATA_NAMES = setOf(".git", ".gradle", "build", "node_modules")
        const val MAX_ACTIVITY_DETAIL_CHARS = 8_000
        const val NOISY_ITEM_SCAN_CHARS = 320
        const val TRANSCRIPT_WRITE_DEBOUNCE_MS = 500L
        const val DIAGNOSTICS_REFRESH_COOLDOWN_MS = 1_000L
        const val RECENT_EXIT_CACHE_MS = 60_000L
        const val MINIMUM_INITIALIZATION_SCREEN_MS = 3_000L
        const val MAX_EDITABLE_FILE_BYTES = 512_000L
        const val MAX_PROJECT_TERMINAL_HISTORY = 100
        const val MAX_PROJECT_TERMINAL_OUTPUT = 200_000
        const val MAX_ANDROID_BUILD_LOG = 300_000
        const val MAX_LOGCAT_LINES = 5_000
        const val MEMORY_PRESSURE_TERMINAL_LINES = 20
        const val MEMORY_PRESSURE_LOGCAT_LINES = 500
        const val MEMORY_PRESSURE_TEXT_CHARS = 64 * 1024
        const val LOGCAT_GRANT_COMMAND = "adb shell pm grant com.jarves.mh android.permission.READ_LOGS"
        const val MAX_ATTACHMENTS_PER_MESSAGE = 5
        const val MAX_PROCESS_OUTPUT_BYTES = 512 * 1024
        const val MAX_ATTACHMENT_BYTES = 25L * 1024L * 1024L
        const val MAX_UPLOAD_BYTES = 100L * 1024L * 1024L
        const val MAX_IMPORTED_PROJECT_BYTES = 8L * 1024L * 1024L * 1024L
        const val MAX_IMPORTED_ZIP_ENTRIES = 100_000
        const val LEGACY_GITHUB_TOKEN_KEY = "GITHUB_APP"
        const val GITHUB_DEVICE_URL = "https://github.com/login/device"
        val GITHUB_DEVICE_CODE = Regex("\\b[A-Z0-9]{4}-[A-Z0-9]{4}\\b")
        const val TEST_PROVIDER_DEFAULTS_VERSION = 1
        const val TEST_OPENROUTER_BASE_URL = "https://openrouter.ai/api"
        const val TEST_OPENROUTER_MODEL = "stealth/ox-alpha"
    }
}
