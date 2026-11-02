package com.jarves.mh.ui.delegate

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import com.jarves.mh.R
import com.jarves.mh.ui.GitHubAuthStatus
import com.jarves.mh.model.Project
import com.jarves.mh.model.ProjectChat
import com.jarves.mh.model.ProjectKind
import com.jarves.mh.model.generateQuickChatIdentity
import com.jarves.mh.network.GitHubClient
import com.jarves.mh.network.GitHubRepository
import com.jarves.mh.network.GitHubTokenPoll
import com.jarves.mh.runtime.RuntimeInstaller
import com.jarves.mh.runtime.readTailText
import com.jarves.mh.ui.MainViewModel
import com.jarves.mh.ui.MainViewModel.Companion.MAX_PROCESS_OUTPUT_BYTES
import com.jarves.mh.ui.sanitizeTerminalOutput
import java.io.File
import java.net.URI
import java.util.LinkedHashMap
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Enterprise Git and GitHub delegate for MindSparQ Jarvis.
 * Manages OAuth device flow, personal access tokens, repository cloning,
 * authenticated remote publishing, and synchronization.
 */
class ProjectGitDelegate(vm: MainViewModel) : BaseViewModelDelegate(vm) {

    @Volatile internal var githubAuthProcess: Process? = null
    internal var githubAuthJob: Job? = null

    fun clonePublicGitRepository(url: String) {
        cloneGitRepository(url = url, repositoryName = null, branch = null, useGitHubCli = false)
    }

    fun cloneGitHubRepository(repository: GitHubRepository) {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) {
            _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.DISCONNECTED, githubMessage = str(R.string.vm_github_connect_again)) }
            return
        }
        cloneGitRepository(repository.cloneUrl, repository.fullName, repository.defaultBranch, useGitHubCli = true)
    }

    private fun cloneGitRepository(url: String, repositoryName: String?, branch: String?, useGitHubCli: Boolean) {
        if (_state.value.gitCloneRunning || _state.value.projectImporting || _state.value.isRunning || _state.value.projectTerminalRunning || _state.value.androidBuildRunning) return
        val normalized = runCatching { validateGitUrl(url) }.getOrElse { error ->
            _state.update { it.copy(toastMessage = error.message ?: str(R.string.vm_invalid_git_url)) }
            return
        }
        _state.update { it.copy(gitCloneRunning = true, gitCloneMessage = str(R.string.vm_connecting_git)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val identity = generateQuickChatIdentity(_state.value.projects.mapTo(mutableSetOf()) { it.slug })
                    val projectId = UUID.randomUUID().toString()
                    val workspace = File(getApplication<Application>().filesDir, "workspaces/$projectId").apply { mkdirs() }
                    val output = File(getApplication<Application>().cacheDir, "git-clone-${System.nanoTime()}.log")
                    try {
                        val environment = mutableMapOf(
                            "GIT_TERMINAL_PROMPT" to "0",
                            "GIT_LFS_SKIP_SMUDGE" to "1",
                            "GH_PROMPT_DISABLED" to "1",
                            "GH_NO_UPDATE_NOTIFIER" to "1",
                        )
                        val installed = installer.installedRuntime()
                        val command = if (useGitHubCli && repositoryName != null) {
                            buildList {
                                addAll(listOf(RuntimeInstaller.GITHUB_CLI_GUEST_PATH, "repo", "clone", repositoryName, ".", "--", "--progress", "--single-branch"))
                                branch?.takeIf(String::isNotBlank)?.let { addAll(listOf("--branch", it)) }
                            }
                        } else {
                            buildList {
                                addAll(listOf("git", "clone", "--progress", "--single-branch"))
                                branch?.takeIf(String::isNotBlank)?.let { addAll(listOf("--branch", it)) }
                                add(normalized)
                                add(".")
                            }
                        }
                        _state.update { it.copy(gitCloneMessage = str(R.string.vm_cloning_named, repositoryName ?: normalized.substringAfterLast('/').removeSuffix(".git"))) }
                        val process = installer.process(
                            installed.proot,
                            installed.rootfs,
                            workspace,
                            environment,
                            command,
                            guestWorkspacePath = "/workspace/${identity.slug}",
                            outputFile = output,
                        )
                        val exit = process.waitFor()
                        val details = output.readTailText(MAX_PROCESS_OUTPUT_BYTES).trim()
                        check(exit == 0) { details.takeLast(600).ifBlank { str(R.string.vm_git_clone_failed_exit, exit) } }
                        val metadata = projectDelegate.detectImportedProjectMetadata(workspace)
                        Project(
                            id = projectId,
                            name = identity.displayName,
                            description = repositoryName?.let { "GitHub · $it" } ?: "Imported Git repository",
                            language = metadata.second,
                            slug = identity.slug,
                            kind = ProjectKind.QUICK_PROJECT,
                        )
                    } catch (error: Throwable) {
                        workspace.deleteRecursively()
                        throw error
                    } finally {
                        output.delete()
                    }
                }
            }
            result.onSuccess { project ->
                val chat = ProjectChat(title = str(R.string.vm_new_chat))
                preferences.saveProjectChats(project.id, listOf(chat))
                _state.update { current -> current.copy(projects = listOf(project) + current.projects) }
                preferences.saveProjects(_state.value.projects)
                openProject(project)
                _state.update { it.copy(gitCloneRunning = false, gitCloneMessage = null, toastMessage = str(R.string.vm_repository_cloned)) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        gitCloneRunning = false,
                        gitCloneMessage = null,
                        toastMessage = str(R.string.vm_clone_failed, error.message?.lineSequence()?.lastOrNull()?.take(180) ?: str(R.string.vm_unknown_error)),
                    )
                }
            }
        }
    }

    private fun validateGitUrl(value: String): String {
        val clean = value.trim()
        val uri = URI(clean)
        require(uri.scheme.equals("https", ignoreCase = true)) { str(R.string.vm_git_https_only) }
        require(uri.userInfo == null && uri.fragment == null && uri.host?.isNotBlank() == true) { str(R.string.vm_git_no_credentials) }
        require(uri.host != "localhost" && uri.host != "127.0.0.1" && uri.host != "::1") { str(R.string.vm_git_local_unsupported) }
        require(uri.path.count { it == '/' } >= 2) { str(R.string.vm_git_url_not_repo) }
        return uri.toASCIIString()
    }

    fun startGitHubLogin() {
        if (_state.value.githubAuthStatus == GitHubAuthStatus.STARTING || _state.value.githubAuthStatus == GitHubAuthStatus.AWAITING_USER) return
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = "Connecting to GitHub...") }
        githubAuthJob = viewModelScope.launch {
            try {
                val client = GitHubClient(getApplication())
                val customClientId = preferences.githubClientId.takeIf(String::isNotBlank)
                val deviceCode = withContext(Dispatchers.IO) {
                    if (customClientId != null) client.requestDeviceCode(customClientId) else client.requestDeviceCode()
                }

                // Immediately update state and copy code to clipboard
                withContext(Dispatchers.Main) {
                    val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("GitHub Code", deviceCode.userCode))
                    // Build auto-fill URL: GitHub supports ?user_code=XXXX-XXXX to pre-fill the code field
                    val autoFillUrl = buildString {
                        append(deviceCode.verificationUri.trimEnd('/'))
                        if (!deviceCode.verificationUri.contains('?')) append("?user_code=") else append("&user_code=")
                        append(deviceCode.userCode)
                    }
                    _state.update {
                        it.copy(
                            githubAuthStatus = GitHubAuthStatus.AWAITING_USER,
                            githubUserCode = deviceCode.userCode,
                            githubVerificationUri = autoFillUrl,
                            githubMessage = "Code ${deviceCode.userCode} is ready. Tap the button below — GitHub will open with your code pre-filled. Just press Authorize.",
                            toastMessage = "GitHub Code: ${deviceCode.userCode} — Auto-filling on GitHub!",
                        )
                    }
                    // Open GitHub with code pre-filled — user just needs to press Authorize
                    openExternalUrl(autoFillUrl)
                }

                // Poll for authorization completion in background
                val intervalMs = (deviceCode.intervalSeconds.coerceAtLeast(5L)) * 1000L
                val startTime = System.currentTimeMillis()
                val timeoutMs = deviceCode.expiresInSeconds * 1000L
                var token: String? = null

                withContext(Dispatchers.IO) {
                    while (System.currentTimeMillis() - startTime < timeoutMs) {
                        delay(intervalMs)
                        when (val poll = client.pollDeviceToken(clientId = customClientId ?: GitHubClient.DEFAULT_CLIENT_ID, deviceCode = deviceCode.deviceCode)) {
                            is GitHubTokenPoll.Success -> {
                                token = poll.accessToken
                                break
                            }
                            is GitHubTokenPoll.Pending -> {
                                // Continue polling
                            }
                            is GitHubTokenPoll.Failure -> {
                                error(poll.message)
                            }
                        }
                    }
                }

                val finalToken = token ?: error("GitHub authorization timed out. Please try again.")
                val account = withContext(Dispatchers.IO) {
                    client.account(finalToken)
                }
                preferences.githubLogin = account.login
                preferences.githubToken = finalToken

                // Configure PRoot git credentials
                runCatching {
                    val runtime = installer.installedRuntime()
                    val ghDir = File(runtime.rootfs, "root/.config/gh").apply { mkdirs() }
                    File(ghDir, "hosts.yml").writeText(
                        "github.com:\n    user: ${account.login}\n    oauth_token: $finalToken\n    git_protocol: https\n"
                    )
                }

                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.CONNECTED,
                        githubLogin = account.login,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = "Connected as @${account.login}",
                        toastMessage = "Successfully connected to GitHub as @${account.login}!",
                    )
                }
                refreshGitHubRepositories()
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.ERROR,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = error.message?.take(240) ?: "GitHub connection failed",
                        toastMessage = "GitHub connection error: ${error.message?.take(100)}",
                    )
                }
            } finally {
                githubAuthJob = null
            }
        }
    }

    fun connectGitHubWithToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = "Connecting with GitHub token...") }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val client = GitHubClient(getApplication())
                    val account = client.account(trimmed)
                    preferences.githubLogin = account.login
                    preferences.githubToken = trimmed

                    // Configure PRoot git credentials
                    runCatching {
                        val runtime = installer.installedRuntime()
                        val ghDir = File(runtime.rootfs, "root/.config/gh").apply { mkdirs() }
                        File(ghDir, "hosts.yml").writeText(
                            "github.com:\n    user: ${account.login}\n    oauth_token: $trimmed\n    git_protocol: https\n"
                        )
                    }
                    account.login
                }
            }
            result.onSuccess { login ->
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.CONNECTED,
                        githubLogin = login,
                        githubUserCode = null,
                        githubVerificationUri = null,
                        githubMessage = "Connected as @$login",
                        toastMessage = "Connected to GitHub as @$login",
                    )
                }
                refreshGitHubRepositories()
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        githubAuthStatus = GitHubAuthStatus.ERROR,
                        githubMessage = err.message ?: "Invalid GitHub token",
                        toastMessage = "GitHub connection failed: ${err.message?.take(80)}",
                    )
                }
            }
        }
    }

    fun generateNewGitHubCode() {
        githubAuthJob?.cancel()
        githubAuthJob = null
        _state.update {
            it.copy(
                githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
                githubUserCode = null,
                githubVerificationUri = null,
                githubMessage = str(R.string.vm_github_new_code),
            )
        }
        startGitHubLogin()
    }

    fun refreshGitHubRepositories() {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) return
        if (_state.value.githubRepositoriesLoading) return
        _state.update { it.copy(githubRepositoriesLoading = true, githubMessage = str(R.string.vm_loading_repositories)) }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val token = preferences.githubToken
                    if (token.isNotBlank()) {
                        GitHubClient(getApplication()).repositories(token)
                    } else {
                        githubRepositoriesFromCli()
                    }
                }
            }
            result.onSuccess { repositories ->
                _state.update {
                    it.copy(
                        githubRepositories = repositories,
                        githubRepositoriesLoading = false,
                        githubMessage = plural(R.plurals.vm_repositories_available, repositories.size, repositories.size),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        githubRepositoriesLoading = false,
                        githubMessage = error.message ?: str(R.string.vm_github_repos_failed),
                    )
                }
            }
        }
    }

    fun disconnectGitHub() {
        if (_state.value.githubAuthStatus == GitHubAuthStatus.STARTING) return
        _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.STARTING, githubMessage = str(R.string.vm_github_signing_out)) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                preferences.githubLogin = ""
                preferences.githubToken = ""
                runCatching {
                    val runtime = installer.installedRuntime()
                    File(runtime.rootfs, "root/.config/gh/hosts.yml").delete()
                }
            }
            _state.update {
                it.copy(
                    githubAuthStatus = GitHubAuthStatus.DISCONNECTED,
                    githubLogin = null,
                    githubUserCode = null,
                    githubVerificationUri = null,
                    githubRepositories = emptyList(),
                    githubMessage = str(R.string.vm_signed_out),
                    toastMessage = "Disconnected from GitHub",
                )
            }
        }
    }

    fun pushProjectToGitHub(
        project: Project,
        repoName: String,
        isPrivate: Boolean,
        onResult: (Boolean, String) -> Unit = { _, _ -> },
    ) {
        if (_state.value.githubAuthStatus != GitHubAuthStatus.CONNECTED) {
            onResult(false, str(R.string.ws_github_signin))
            return
        }
        val githubUser = _state.value.githubLogin.orEmpty().ifBlank { "developer" }
        val token = preferences.githubToken

        val targetRepoName = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-").ifBlank { project.slug }
        _state.update { it.copy(gitCloneRunning = true, gitCloneMessage = "Publishing to GitHub: $targetRepoName...") }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val runtime = installer.installedRuntime()
                    val workspace = projectWorkspaceRoot(project)

                    // 1. Pre-create repository via GitHub REST API if token exists
                    if (token.isNotBlank()) {
                        runCatching {
                            GitHubClient(getApplication()).createRepository(
                                token = token,
                                name = targetRepoName,
                                description = "Generated by MindSparQ Jarvis",
                                isPrivate = isPrivate,
                            )
                        }
                    }

                    val env = githubCliEnvironment() + mapOf(
                        "GIT_TERMINAL_PROMPT" to "0",
                        "GIT_COMMITTER_NAME" to githubUser,
                        "GIT_COMMITTER_EMAIL" to "$githubUser@users.noreply.github.com",
                        "GIT_AUTHOR_NAME" to githubUser,
                        "GIT_AUTHOR_EMAIL" to "$githubUser@users.noreply.github.com",
                    )

                    fun runWorkspaceCmd(cmd: String): Pair<Int, String> {
                        val outputFile = File(getApplication<Application>().cacheDir, "gh-push-${System.nanoTime()}.log")
                        return try {
                            val proc = installer.process(
                                runtime.proot,
                                runtime.rootfs,
                                workspace,
                                env,
                                listOf("/usr/bin/bash", "-lc", cmd),
                                projectGuestRoot(project),
                                outputFile = outputFile,
                            )
                            val exit = proc.waitFor()
                            val out = sanitizeTerminalOutput(outputFile.readTailText(64 * 1024)).trim()
                            exit to out
                        } finally {
                            outputFile.delete()
                        }
                    }

                    // 2. Initialize git if repository doesn't have .git
                    val (isGitExit, _) = runWorkspaceCmd("git rev-parse --is-inside-work-tree")
                    if (isGitExit != 0) {
                        runWorkspaceCmd("git init -b main")
                    }
                    runWorkspaceCmd("git config user.name \"$githubUser\"")
                    runWorkspaceCmd("git config user.email \"$githubUser@users.noreply.github.com\"")

                    // 3. Stage and commit all project files
                    runWorkspaceCmd("git add -A")
                    runWorkspaceCmd("git commit -m \"Initial commit from MindSparQ Jarvis\"")

                    // 4. Configure authenticated remote origin and push
                    val authenticatedRemote = if (token.isNotBlank()) {
                        "https://$githubUser:$token@github.com/$githubUser/$targetRepoName.git"
                    } else {
                        "https://github.com/$githubUser/$targetRepoName.git"
                    }
                    runWorkspaceCmd("git remote remove origin")
                    runWorkspaceCmd("git remote add origin $authenticatedRemote")

                    val (pExit, pOut) = runWorkspaceCmd("git push -u origin HEAD")
                    check(pExit == 0) { pOut.ifBlank { "Git push failed with exit code $pExit" } }
                    "https://github.com/$githubUser/$targetRepoName"
                }
            }
            _state.update { it.copy(gitCloneRunning = false, gitCloneMessage = null) }
            result.onSuccess { url ->
                _state.update { it.copy(toastMessage = "Published to GitHub: $url") }
                refreshGitHubRepositories()
                onResult(true, url)
            }.onFailure { err ->
                val errorMsg = err.message?.lineSequence()?.lastOrNull { it.isNotBlank() } ?: "Push failed"
                _state.update { it.copy(toastMessage = "GitHub Push failed: $errorMsg") }
                onResult(false, errorMsg)
            }
        }
    }

    internal suspend fun refreshGitHubConnection() = withContext(Dispatchers.IO) {
        if (!installer.isGitHubCliInstalled()) return@withContext
        val login = runCatching { githubAccountLogin() }.getOrNull()
        if (login.isNullOrBlank()) {
            preferences.githubLogin = ""
            _state.update { it.copy(githubAuthStatus = GitHubAuthStatus.DISCONNECTED, githubLogin = null) }
        } else {
            preferences.githubLogin = login
            _state.update {
                it.copy(
                    githubAuthStatus = GitHubAuthStatus.CONNECTED,
                    githubLogin = login,
                    githubMessage = str(R.string.vm_github_connected_as, login),
                )
            }
        }
    }

    private fun githubCliEnvironment(): Map<String, String> = mapOf(
        "GH_PROMPT_DISABLED" to "1",
        "GH_NO_UPDATE_NOTIFIER" to "1",
        "BROWSER" to "/bin/false",
    )

    private fun runGitHubCli(arguments: List<String>): Pair<Int, String> {
        check(installer.isGitHubCliInstalled()) { str(R.string.vm_github_cli_missing) }
        val runtime = installer.installedRuntime()
        val outputFile = File(getApplication<Application>().cacheDir, "github-cli-${System.nanoTime()}.log")
        val workspace = File(getApplication<Application>().filesDir, "workspaces/github-auth").apply { mkdirs() }
        return try {
            val process = installer.process(
                runtime.proot,
                runtime.rootfs,
                workspace,
                githubCliEnvironment(),
                listOf(RuntimeInstaller.GITHUB_CLI_GUEST_PATH) + arguments,
                guestWorkspacePath = "/workspace/github-auth",
                outputFile = outputFile,
            )
            val exit = process.waitFor()
            exit to sanitizeTerminalOutput(outputFile.readTailText(MAX_PROCESS_OUTPUT_BYTES)).trim()
        } finally {
            outputFile.delete()
        }
    }

    private fun githubAccountLogin(): String? {
        val (exit, output) = runGitHubCli(listOf("api", "user", "--jq", ".login"))
        return output.lineSequence().lastOrNull { it.isNotBlank() }?.trim().takeIf { exit == 0 && !it.isNullOrBlank() }
    }

    private fun githubRepositoriesFromCli(): List<GitHubRepository> {
        val endpoint = "user/repos?visibility=all&affiliation=owner,collaborator,organization_member&sort=updated&per_page=100"
        val (exit, output) = runGitHubCli(listOf("api", "--paginate", "--slurp", endpoint))
        check(exit == 0) { output.lineSequence().lastOrNull { it.isNotBlank() } ?: str(R.string.vm_github_repos_failed) }
        val pages = JSONArray(output)
        val repositories = LinkedHashMap<String, GitHubRepository>()
        for (pageIndex in 0 until pages.length()) {
            val page = pages.optJSONArray(pageIndex) ?: continue
            for (index in 0 until page.length()) {
                val item = page.optJSONObject(index) ?: continue
                val fullName = item.optString("full_name").takeIf(String::isNotBlank) ?: continue
                repositories[fullName] = GitHubRepository(
                    fullName = fullName,
                    cloneUrl = item.optString("clone_url", "https://github.com/$fullName.git"),
                    private = item.optBoolean("private"),
                    defaultBranch = item.optString("default_branch", "main"),
                    description = item.optString("description"),
                    updatedAt = item.optString("updated_at"),
                )
            }
        }
        return repositories.values.toList()
    }

    private fun openExternalUrl(url: String) {
        runCatching {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            _state.update { state -> state.copy(toastMessage = str(R.string.vm_browser_open_failed_url)) }
        }
    }

    private fun startGitHubForegroundOperation() {
        ContextCompat.startForegroundService(
            getApplication(),
            Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java)
                .setAction(com.jarves.mh.runtime.RuntimeExecutionService.ACTION_START)
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_PROJECT_NAME, str(R.string.vm_notif_github_signin))
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_TITLE, str(R.string.vm_notif_connecting_github))
                .putExtra(com.jarves.mh.runtime.RuntimeExecutionService.EXTRA_CAN_STOP, false),
        )
    }

    private fun stopGitHubForegroundOperation() {
        runCatching {
            getApplication<Application>().startService(
                Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java)
                    .setAction(com.jarves.mh.runtime.RuntimeExecutionService.ACTION_CANCELLED),
            )
        }.onFailure {
            getApplication<Application>().stopService(
                Intent(getApplication(), com.jarves.mh.runtime.RuntimeExecutionService::class.java),
            )
        }
    }
}

