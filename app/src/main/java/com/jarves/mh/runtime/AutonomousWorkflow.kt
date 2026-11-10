package com.jarves.mh.runtime

import org.json.JSONObject

internal enum class ReviewVerdict { APPROVE, REVISE }

internal data class AgentReview(val verdict: ReviewVerdict, val findings: String)

internal data class AutonomousWorkflowResult(
    val plan: String,
    val review: AgentReview,
    val revisionMade: Boolean,
)

internal class AutonomousWorkflowCoordinator {
    suspend fun run(
        task: String,
        planner: suspend (systemPrompt: String, userPrompt: String) -> String,
        implementer: suspend (prompt: String) -> Unit,
        reviewer: suspend (systemPrompt: String, userPrompt: String) -> AgentReview,
    ): AutonomousWorkflowResult {
        val plan = planner(
            "You are the planning agent. Inspect the user's request conceptually, identify a minimal implementation sequence and relevant validation. Do not claim to inspect files or execute tools. Return a concise actionable plan.",
            task,
        ).trim().ifBlank { error("The planning agent returned an empty plan.") }

        implementer(implementationPrompt(task, plan))
        var review = reviewer(
            reviewSystemPrompt,
            reviewPrompt(task, plan),
        )
        var revisionMade = false
        if (review.verdict == ReviewVerdict.REVISE) {
            revisionMade = true
            implementer(revisionPrompt(task, plan, review.findings))
            review = reviewer(
                reviewSystemPrompt,
                reviewPrompt(task, plan, review.findings),
            )
        }
        return AutonomousWorkflowResult(plan, review, revisionMade)
    }

    fun parseReview(response: String): AgentReview {
        val jsonText = response.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(jsonText)
        val verdict = when (json.optString("verdict").trim().uppercase()) {
            "APPROVE" -> ReviewVerdict.APPROVE
            "REVISE" -> ReviewVerdict.REVISE
            else -> error("The reviewer returned an invalid verdict; expected APPROVE or REVISE.")
        }
        val findings = json.optString("findings").trim()
        require(findings.isNotBlank()) { "The reviewer returned no findings." }
        return AgentReview(verdict, findings.take(MAX_REVIEW_FINDINGS_CHARS))
    }

    private fun implementationPrompt(task: String, plan: String) = buildString {
        appendLine("Implement the user's request in this project workspace.")
        appendLine("Follow this plan:")
        appendLine(plan)
        appendLine()
        appendLine("User request:")
        appendLine(task)
    }

    private fun revisionPrompt(task: String, plan: String, findings: String) = buildString {
        appendLine("Make one bounded revision to address the review findings below.")
        appendLine("Do not expand scope or make unrelated changes.")
        appendLine("Original user request:")
        appendLine(task)
        appendLine()
        appendLine("Plan:")
        appendLine(plan)
        appendLine()
        appendLine("Reviewer findings:")
        appendLine(findings)
    }

    private fun reviewPrompt(task: String, plan: String, priorFindings: String? = null) = buildString {
        appendLine("Review the project changes for correctness and relevant test coverage.")
        appendLine("Do not edit files or claim to run tests. Return exactly JSON with keys verdict and findings.")
        appendLine("verdict must be APPROVE or REVISE. Use REVISE only for a concrete actionable issue.")
        appendLine()
        appendLine("User request:")
        appendLine(task)
        appendLine()
        appendLine("Planner output:")
        appendLine(plan)
        priorFindings?.let {
            appendLine()
            appendLine("Previously reported findings (check whether these are resolved):")
            appendLine(it)
        }
    }

    private companion object {
        const val MAX_REVIEW_FINDINGS_CHARS = 8_000
        const val reviewSystemPrompt = "You are an independent code reviewer. Be evidence-based and concise."
    }
}
