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
        Regex("""\brm\s+-[rR]*f[rR]*\s+/(?:\s+|$)"""),
        Regex("""\bmkfs(?:\.[\w-]+)?\b"""),
        Regex("""\bdd\s+if=.*of=/dev/"""),
        Regex("""\bshutdown\b"""),
        Regex("""\breboot\b""")
    )

    private val SAFE_COMMAND_PATTERNS = listOf(
        Regex("""^git\s+(status|diff|log|branch|show)"""),
        Regex("""^(ls|pwd|cat|head|tail|find|grep|ripgrep|rg)\b"""),
        Regex("""^(pytest|vitest|jest|flutter\s+test|ctest)\b"""),
        Regex("""^\./gradlew\s+test\w*"""),
        Regex("""^echo\b""")
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

    fun evaluatePathAccess(workspaceRoot: String, targetPath: String): PolicyDecision {
        val normalizedRoot = java.io.File(workspaceRoot).canonicalPath
        val normalizedTarget = java.io.File(targetPath).canonicalPath

        return if (normalizedTarget == normalizedRoot || normalizedTarget.startsWith("$normalizedRoot${java.io.File.separator}")) {
            PolicyDecision(PolicyAction.ALLOW, "Path is within workspace boundaries")
        } else {
            PolicyDecision(PolicyAction.DENY, "Path traversal attempt outside workspace jail: $targetPath")
        }
    }
}

