package com.mindsparq.jarvis.core.policy

import com.mindsparq.jarvis.core.model.ExecutionMode

enum class PolicyAction {
    ALLOW,
    ASK,
    DENY
}

data class PolicyDecision(
    val action: PolicyAction,
    val reason: String
)

object PolicyEngine {

    private val HARD_DENY_PATTERNS = listOf(
        Regex("""\brm\s+-[rR]*f[rR]*\s+/(?:\*|\s+|$)"""),
        Regex("""\bmkfs(?:\.[\w-]+)?\b"""),
        Regex("""\bdd\s+if=.*of=/dev/"""),
        Regex("""\bshutdown\b"""),
        Regex("""\breboot\b"""),
        Regex("""\bchmod\s+-[rR]*\s*777\s+/"""),
        Regex(""":\(\)\{\s*:\|:&\s*\};:"""),
        Regex("""(curl|wget).*(\|\s*|>\s*)(sudo\s+)?(sh|bash)""")
    )

    private val SAFE_COMMAND_PATTERNS = listOf(
        Regex("""^git\s+(status|diff|log|branch|show)"""),
        Regex("""^(ls|pwd|cat|head|tail|find|grep|ripgrep|rg)\b"""),
        Regex("""^(pytest|vitest|jest|flutter\s+test|ctest)\b"""),
        Regex("""^\./gradlew\s+test\w*"""),
        Regex("""^echo\b""")
    )

    private val DESTRUCTIVE_COMMAND_PATTERNS = listOf(
        Regex("""\brm\s+-[rR]*f"""),
        Regex("""\bgit\s+reset\s+--hard"""),
        Regex("""\bgit\s+clean\s+-[fF]"""),
        Regex("""\bgit\s+push\s+.*--force"""),
        Regex("""\bpkill\s+-f"""),
        Regex("""\bkill\s+\$\(pgrep"""),
    )

    fun evaluateCommand(command: String, mode: ExecutionMode): PolicyDecision {
        val trimmed = command.trim()

        // 1. Check Hard Deny patterns
        for (pattern in HARD_DENY_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return PolicyDecision(
                    PolicyAction.DENY,
                    "Command contains dangerous system destruction pattern: ${pattern.pattern}"
                )
            }
        }

        // 2. Evaluate against active Execution Mode
        return when (mode) {
            ExecutionMode.ASK -> PolicyDecision(
                PolicyAction.DENY,
                "Command execution is strictly disabled in Ask (read-only) mode"
            )
            ExecutionMode.PLAN -> PolicyDecision(
                PolicyAction.DENY,
                "Command execution is disabled in Plan mode; only documentation writes are permitted"
            )
            ExecutionMode.WORK -> {
                val isSafe = SAFE_COMMAND_PATTERNS.any { it.containsMatchIn(trimmed) }
                if (isSafe) {
                    PolicyDecision(PolicyAction.ALLOW, "Matched safe command allowlist")
                } else {
                    PolicyDecision(PolicyAction.ASK, "Command requires explicit user confirmation before execution")
                }
            }
        }
    }

    fun evaluateTerminalCommand(command: String, workspaceRoot: String? = null): PolicyDecision {
        val trimmed = command.trim()

        // 1. Hard deny dangerous destruction patterns
        for (pattern in HARD_DENY_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return PolicyDecision(
                    PolicyAction.DENY,
                    "Command blocked by security policy: ${pattern.pattern}"
                )
            }
        }

        // 2. Path jail verification: detect escape attempts outside workspace root
        if (!workspaceRoot.isNullOrBlank()) {
            val hasRootEscape = Regex("""\b(cd\s+|cat\s+|rm\s+|mv\s+|cp\s+)(/(etc|var|usr|bin|sbin|proc|sys|dev|root|system)|(\.\./){3,})""").containsMatchIn(trimmed)
            if (hasRootEscape) {
                return PolicyDecision(
                    PolicyAction.DENY,
                    "Path traversal attempt outside workspace jail"
                )
            }
        }

        // 3. Check destructive operations that require user confirmation
        for (pattern in DESTRUCTIVE_COMMAND_PATTERNS) {
            if (pattern.containsMatchIn(trimmed)) {
                return PolicyDecision(
                    PolicyAction.ASK,
                    "Destructive command requires confirmation: ${pattern.pattern}"
                )
            }
        }

        return PolicyDecision(PolicyAction.ALLOW, "Permitted terminal command")
    }

    fun evaluatePathAccess(workspaceRoot: String, targetPath: String): PolicyDecision {
        val normalizedRoot = java.io.File(workspaceRoot).canonicalPath
        val normalizedTarget = java.io.File(targetPath).canonicalPath

        return if (normalizedTarget == normalizedRoot || normalizedTarget.startsWith("$normalizedRoot${java.io.File.separator}")) {
            PolicyDecision(PolicyAction.ALLOW, "Path is within workspace boundaries")
        } else {
            PolicyDecision(PolicyAction.DENY, "Path traversal attempt outside workspace jail: $targetPath")
        }
    }

    fun evaluateAgentTool(
        toolName: String,
        command: String?,
        targetPaths: List<String>,
        workspaceRoot: String,
        guestWorkspaceRoot: String,
    ): PolicyDecision {
        val root = java.io.File(workspaceRoot).canonicalFile
        val guestRoot = guestWorkspaceRoot.trimEnd('/')
        if (workspaceRoot.isBlank() || guestRoot.isBlank() || targetPaths.any(String::isBlank)) {
            return PolicyDecision(PolicyAction.DENY, "The agent tool request has an invalid workspace path")
        }

        for (path in targetPaths) {
            val candidate = path.trim()
            val relative = when {
                candidate == guestRoot -> ""
                candidate.startsWith("$guestRoot/") -> candidate.removePrefix("$guestRoot/")
                candidate.startsWith("/") -> return PolicyDecision(
                    PolicyAction.DENY,
                    "Agent tool path is outside the project workspace",
                )
                else -> candidate
            }
            val decision = evaluatePathAccess(root.path, java.io.File(root, relative).path)
            if (decision.action != PolicyAction.ALLOW) return decision
        }

        return when (toolName.lowercase()) {
            "bash" -> evaluateAgentCommand(command.orEmpty(), guestRoot)
            "write", "edit", "notebookedit", "read", "glob", "grep", "listfiles" ->
                if (targetPaths.isEmpty()) {
                    PolicyDecision(PolicyAction.ASK, "The tool did not identify a project path")
                } else {
                    PolicyDecision(PolicyAction.ALLOW, "Tool paths are confined to the project workspace")
                }
            else -> PolicyDecision(PolicyAction.ASK, "This agent tool requires explicit user approval")
        }
    }

    private fun evaluateAgentCommand(command: String, guestWorkspaceRoot: String): PolicyDecision {
        val trimmed = command.trim()
        val hardDeny = evaluateCommand(trimmed, ExecutionMode.WORK)
        if (hardDeny.action == PolicyAction.DENY) return hardDeny

        if (Regex("""(^|\s|/)\.\.(/|\s|$)""").containsMatchIn(trimmed)) {
            return PolicyDecision(PolicyAction.DENY, "Path traversal is not allowed in agent commands")
        }
        val externalPath = Regex("""(?:^|[\s"'=(])(/[^ \t\r\n"'(),;]*)""")
            .findAll(trimmed)
            .map { it.groupValues[1] }
            .firstOrNull { path -> path != guestWorkspaceRoot && !path.startsWith("$guestWorkspaceRoot/") }
        if (externalPath != null) {
            return PolicyDecision(PolicyAction.DENY, "Agent command references a path outside the project workspace")
        }

        if (Regex("""[;&|<>`\n\r]|\$\(""").containsMatchIn(trimmed)) {
            return PolicyDecision(PolicyAction.ASK, "Compound shell commands require explicit user approval")
        }

        val safeExactCommands = setOf("pwd", "ls", "git status", "git diff", "git log", "git branch")
        return if (trimmed in safeExactCommands) {
            PolicyDecision(PolicyAction.ALLOW, "Read-only command is on the agent allowlist")
        } else {
            PolicyDecision(PolicyAction.ASK, "Command requires explicit user approval")
        }
    }
}
