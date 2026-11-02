package com.jarves.mh.ui.delegate

import android.app.Application
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.jarves.mh.model.Project
import com.jarves.mh.model.WorkspaceEntry
import com.jarves.mh.ui.AppStrings
import com.jarves.mh.ui.AppUiState
import com.jarves.mh.ui.MainViewModel
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

abstract class BaseViewModelDelegate(internal val vm: MainViewModel) {
    internal val app: Application get() = vm.getApplication()
    internal inline fun <reified T : Application> getApplication(): T = vm.getApplication()
    internal val viewModelScope: CoroutineScope get() = vm.viewModelScope
    internal val _state: MutableStateFlow<AppUiState> get() = vm._state
    internal val state: StateFlow<AppUiState> get() = vm.state

    internal fun str(@StringRes id: Int, vararg args: Any): String = vm.str(id, *args)
    internal fun plural(@PluralsRes id: Int, count: Int, vararg args: Any): String = vm.plural(id, count, *args)

    internal val vault get() = vm.vault
    internal val preferences get() = vm.preferences
    internal val keyPoolManager get() = vm.keyPoolManager
    internal val workspaceCheckpoints get() = vm.workspaceCheckpoints
    internal val integrityScheduler get() = vm.integrityScheduler
    internal val installer get() = vm.installer
    internal val claudeRuntime get() = vm.claudeRuntime
    internal val dshRuntime get() = vm.dshRuntime
    internal val antigravityRuntime get() = vm.antigravityRuntime
    internal val antigravityAuthController get() = vm.antigravityAuthController
    internal val agentRegistry get() = vm.agentRegistry
    internal val providerApi get() = vm.providerApi
    internal val offlineModelManager get() = vm.offlineModelManager

    internal fun activeRuntime() = vm.activeRuntime()
    internal fun projectGuestRoot(project: Project): String = vm.projectGuestRoot(project)
    internal fun projectWorkspaceRoot(project: Project): File = vm.projectWorkspaceRoot(project)
    internal fun syncKeyPool() = vm.syncKeyPool()
    internal fun appUpdater() = vm.appUpdater()

    internal val terminalDelegate: TerminalDelegate get() = vm.terminalDelegate
    internal val androidDelegate: AndroidBuildDelegate get() = vm.androidDelegate
    internal val agentDelegate: AgentDelegate get() = vm.agentDelegate
    internal val projectDelegate: ProjectDelegate get() = vm.projectDelegate
    internal val gitDelegate: ProjectGitDelegate get() = vm.gitDelegate
    internal val workspaceDelegate: WorkspaceDelegate get() = vm.workspaceDelegate
    internal val chatDelegate: ChatDelegate get() = vm.chatDelegate

    open fun refreshProjectFiles() {
        workspaceDelegate.refreshProjectFiles()
    }

    open fun persistMessages(includeLiveProcess: Boolean = true) {
        chatDelegate.persistMessages(includeLiveProcess)
    }

    open fun openProject(project: Project) {
        projectDelegate.openProject(project)
    }
}
