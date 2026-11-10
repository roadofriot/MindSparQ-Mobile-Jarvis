package com.jarves.mh.runtime

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONException

class AutonomousWorkflowTest {

    private fun assertThrows(block: () -> Unit) {
        var threw = false
        try {
            block()
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Expected an exception to be thrown", threw)
    }

    @Test
    fun coordinatorAllowsOnlyOneImplementationRevision() = runBlocking {
        val coordinator = AutonomousWorkflowCoordinator()
        val implementationPrompts = mutableListOf<String>()
        var reviewCount = 0

        val result = coordinator.run(
            task = "Add a feature",
            planner = { _, _ -> "Inspect, implement, test." },
            implementer = { implementationPrompts += it },
            reviewer = { _, _ ->
                reviewCount += 1
                if (reviewCount == 1) {
                    AgentReview(ReviewVerdict.REVISE, "Add a missing regression test.")
                } else {
                    AgentReview(ReviewVerdict.APPROVE, "The regression test is present.")
                }
            },
        )

        assertEquals(2, implementationPrompts.size)
        assertTrue(implementationPrompts.last().contains("Add a missing regression test."))
        assertEquals(2, reviewCount)
        assertTrue(result.revisionMade)
        assertEquals(ReviewVerdict.APPROVE, result.review.verdict)
    }

    @Test
    fun coordinatorRejectsEmptyPlanFromPlanner() = runBlocking {
        val coordinator = AutonomousWorkflowCoordinator()
        var threw = false
        try {
            coordinator.run(
                task = "Add a feature",
                planner = { _, _ -> "" },
                implementer = { },
                reviewer = { _, _ -> AgentReview(ReviewVerdict.APPROVE, "ok") },
            )
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Expected an exception to be thrown", threw)
    }

    @Test
    fun coordinatorPropagatesImplementerFailure() = runBlocking {
        val coordinator = AutonomousWorkflowCoordinator()
        var threw = false
        try {
            coordinator.run(
                task = "Add a feature",
                planner = { _, _ -> "Inspect, implement, test." },
                implementer = { error("Implementer crashed") },
                reviewer = { _, _ -> AgentReview(ReviewVerdict.APPROVE, "ok") },
            )
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Expected an exception to be thrown", threw)
    }

    @Test
    fun coordinatorPropagatesReviewerFailure() = runBlocking {
        val coordinator = AutonomousWorkflowCoordinator()
        var threw = false
        try {
            coordinator.run(
                task = "Add a feature",
                planner = { _, _ -> "Inspect, implement, test." },
                implementer = { },
                reviewer = { _, _ -> error("Reviewer crashed") },
            )
        } catch (_: Exception) {
            threw = true
        }
        assertTrue("Expected an exception to be thrown", threw)
    }

    @Test
    fun reviewParserRejectsInvalidVerdict() {
        assertThrows {
            AutonomousWorkflowCoordinator().parseReview(
                """{"verdict":"MAYBE","findings":"Undecided."}""",
            )
        }
    }

    @Test
    fun reviewParserRejectsNonJson() {
        assertThrows {
            AutonomousWorkflowCoordinator().parseReview("This is not JSON at all.")
        }
    }

    @Test
    fun reviewParserRejectsMissingFindings() {
        assertThrows {
            AutonomousWorkflowCoordinator().parseReview("""{"verdict":"APPROVE"}""")
        }
    }

    @Test
    fun coordinatorBoundsToExactlyOneRevisionEvenIfFindingsUnresolved() = runBlocking {
        val coordinator = AutonomousWorkflowCoordinator()
        val implementationPrompts = mutableListOf<String>()
        var reviewCount = 0

        val result = coordinator.run(
            task = "Add a feature",
            planner = { _, _ -> "Inspect, implement, test." },
            implementer = { implementationPrompts += it },
            reviewer = { _, _ ->
                reviewCount += 1
                // Reviewer keeps requesting revisions; coordinator must still stop after one.
                AgentReview(ReviewVerdict.REVISE, "Still missing a test.")
            },
        )

        // Exactly one implementation call for the initial plan, plus one revision call.
        assertEquals(2, implementationPrompts.size)
        assertEquals(2, reviewCount)
        assertTrue(result.revisionMade)
        assertEquals(ReviewVerdict.REVISE, result.review.verdict)
    }

    @Test
    fun coordinatorApprovesOnFirstReviewWithoutRevision() = runBlocking {
        val coordinator = AutonomousWorkflowCoordinator()
        val implementationPrompts = mutableListOf<String>()

        val result = coordinator.run(
            task = "Add a feature",
            planner = { _, _ -> "Inspect, implement, test." },
            implementer = { implementationPrompts += it },
            reviewer = { _, _ -> AgentReview(ReviewVerdict.APPROVE, "Looks good.") },
        )

        assertEquals(1, implementationPrompts.size)
        assertFalse(result.revisionMade)
        assertEquals(ReviewVerdict.APPROVE, result.review.verdict)
    }

    @Test
    fun reviewParserHandlesJsonCodeFence() {
        val review = AutonomousWorkflowCoordinator().parseReview(
            """```json
            {"verdict":"APPROVE","findings":"All clear."}
            ```""",
        )

        assertEquals(ReviewVerdict.APPROVE, review.verdict)
        assertEquals("All clear.", review.findings)
    }

    @Test
    fun reviewParserTruncatesVeryLongFindings() {
        val longFindings = "x".repeat(20_000)
        val review = AutonomousWorkflowCoordinator().parseReview(
            """{"verdict":"REVISE","findings":"$longFindings"}""",
        )

        assertTrue(review.findings.length <= 8_000)
        assertTrue(review.findings.length < longFindings.length)
    }
}
