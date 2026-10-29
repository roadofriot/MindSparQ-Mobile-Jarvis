package com.mindsparq.jarvis.engine.loop

import com.mindsparq.jarvis.core.model.TaskState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskOrchestratorTest {

    @Test
    fun testHappyPathLifecycle() {
        val orchestrator = TaskOrchestrator(TaskContext(projectId = "p1", title = "Add unit tests"))
        assertEquals(TaskState.QUEUED, orchestrator.snapshot.value.state)

        assertTrue(orchestrator.startInspection())
        assertEquals(TaskState.INSPECTING, orchestrator.snapshot.value.state)

        assertTrue(orchestrator.proceedToPlanning())
        assertEquals(TaskState.PLANNING, orchestrator.snapshot.value.state)

        assertTrue(orchestrator.requestApproval("diff --git a/file b/file"))
        assertEquals(TaskState.AWAITING_APPROVAL, orchestrator.snapshot.value.state)

        assertTrue(orchestrator.onApprovalGranted())
        assertEquals(TaskState.IMPLEMENTING, orchestrator.snapshot.value.state)

        assertTrue(orchestrator.runTests())
        assertEquals(TaskState.TESTING, orchestrator.snapshot.value.state)

        val finalState = orchestrator.onTestsResult(allPassed = true, testOutputSummary = "10/10 passed", exitCode = 0)
        assertEquals(TaskState.COMPLETED, finalState)
        assertEquals(TaskState.COMPLETED, orchestrator.snapshot.value.state)
        assertTrue(orchestrator.snapshot.value.evidenceList.isNotEmpty())
    }

    @Test
    fun testApprovalDenialFlow() {
        val orchestrator = TaskOrchestrator(TaskContext(projectId = "p1", title = "Dangerous refactor"))
        orchestrator.startInspection()
        orchestrator.proceedToPlanning()
        orchestrator.requestApproval("rm -rf old_module")

        assertTrue(orchestrator.onApprovalDenied("Too risky"))
        assertEquals(TaskState.CANCELLED, orchestrator.snapshot.value.state)
        assertNotNull(orchestrator.snapshot.value.failureReason)
    }

    @Test
    fun testBoundedRepairLoopLimit() {
        val orchestrator = TaskOrchestrator(TaskContext(projectId = "p1", title = "Fix bug", maxRepairAttempts = 3))
        orchestrator.startInspection()
        orchestrator.proceedToPlanning()
        orchestrator.requestApproval("fix bug")
        orchestrator.onApprovalGranted()

        // Attempt 1 fails -> goes to DEBUGGING
        orchestrator.runTests()
        val s1 = orchestrator.onTestsResult(allPassed = false, testOutputSummary = "Test failed", exitCode = 1)
        assertEquals(TaskState.DEBUGGING, s1)
        assertEquals(1, orchestrator.snapshot.value.repairAttempts)

        // Attempt 2 fails -> goes to DEBUGGING
        orchestrator.runTests()
        val s2 = orchestrator.onTestsResult(allPassed = false, testOutputSummary = "Test failed again", exitCode = 1)
        assertEquals(TaskState.DEBUGGING, s2)
        assertEquals(2, orchestrator.snapshot.value.repairAttempts)

        // Attempt 3 fails -> goes to DEBUGGING
        orchestrator.runTests()
        val s3 = orchestrator.onTestsResult(allPassed = false, testOutputSummary = "Test still failing", exitCode = 1)
        assertEquals(TaskState.DEBUGGING, s3)
        assertEquals(3, orchestrator.snapshot.value.repairAttempts)

        // Attempt 4 fails -> EXCEEDS LIMIT -> goes to FAILED!
        orchestrator.runTests()
        val s4 = orchestrator.onTestsResult(allPassed = false, testOutputSummary = "Test failed completely", exitCode = 1)
        assertEquals(TaskState.FAILED, s4)
        assertEquals(TaskState.FAILED, orchestrator.snapshot.value.state)
        assertTrue(orchestrator.snapshot.value.failureReason!!.contains("Bounded repair limit reached"))
    }
}

