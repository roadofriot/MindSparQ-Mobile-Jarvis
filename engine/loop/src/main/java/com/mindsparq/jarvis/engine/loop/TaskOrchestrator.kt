package com.mindsparq.jarvis.engine.loop

import com.mindsparq.jarvis.core.model.EvidenceKind
import com.mindsparq.jarvis.core.model.EvidenceRecord
import com.mindsparq.jarvis.core.model.ExecutionMode
import com.mindsparq.jarvis.core.model.TaskState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class TaskContext(
    val taskId: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String,
    val mode: ExecutionMode = ExecutionMode.WORK,
    val maxSteps: Int = 60,
    val maxRepairAttempts: Int = 3
)

data class TaskSnapshot(
    val taskId: String,
    val state: TaskState,
    val currentStep: Int,
    val repairAttempts: Int,
    val evidenceList: List<EvidenceRecord>,
    val failureReason: String? = null
)

class TaskOrchestrator(private val context: TaskContext) {

    private val _snapshot = MutableStateFlow(
        TaskSnapshot(
            taskId = context.taskId,
            state = TaskState.QUEUED,
            currentStep = 0,
            repairAttempts = 0,
            evidenceList = emptyList()
        )
    )
    val snapshot: StateFlow<TaskSnapshot> = _snapshot.asStateFlow()

    fun startInspection(): Boolean {
        if (_snapshot.value.state != TaskState.QUEUED) return false
        _snapshot.value = _snapshot.value.copy(
            state = TaskState.INSPECTING,
            currentStep = _snapshot.value.currentStep + 1
        )
        return true
    }

    fun proceedToPlanning(): Boolean {
        if (_snapshot.value.state != TaskState.INSPECTING) return false
        _snapshot.value = _snapshot.value.copy(
            state = TaskState.PLANNING,
            currentStep = _snapshot.value.currentStep + 1
        )
        return true
    }

    fun requestApproval(diffSummary: String): Boolean {
        if (_snapshot.value.state != TaskState.PLANNING && _snapshot.value.state != TaskState.IMPLEMENTING) return false
        recordEvidence(EvidenceKind.DIFF, diffSummary)
        _snapshot.value = _snapshot.value.copy(
            state = TaskState.AWAITING_APPROVAL,
            currentStep = _snapshot.value.currentStep + 1
        )
        return true
    }

    fun onApprovalGranted(): Boolean {
        if (_snapshot.value.state != TaskState.AWAITING_APPROVAL) return false
        recordEvidence(EvidenceKind.APPROVAL, "User approved implementation plan")
        _snapshot.value = _snapshot.value.copy(
            state = TaskState.IMPLEMENTING,
            currentStep = _snapshot.value.currentStep + 1
        )
        return true
    }

    fun onApprovalDenied(reason: String): Boolean {
        if (_snapshot.value.state != TaskState.AWAITING_APPROVAL) return false
        recordEvidence(EvidenceKind.APPROVAL, "User denied action: $reason")
        _snapshot.value = _snapshot.value.copy(
            state = TaskState.CANCELLED,
            failureReason = "User rejected approval: $reason"
        )
        return true
    }

    fun runTests(): Boolean {
        if (_snapshot.value.state != TaskState.IMPLEMENTING && _snapshot.value.state != TaskState.DEBUGGING) return false
        _snapshot.value = _snapshot.value.copy(
            state = TaskState.TESTING,
            currentStep = _snapshot.value.currentStep + 1
        )
        return true
    }

    fun onTestsResult(allPassed: Boolean, testOutputSummary: String, exitCode: Int): TaskState {
        if (_snapshot.value.state != TaskState.TESTING) return _snapshot.value.state

        recordEvidence(EvidenceKind.TEST_RUN, testOutputSummary, exitCode = exitCode)

        return if (allPassed) {
            _snapshot.value = _snapshot.value.copy(
                state = TaskState.VERIFYING,
                currentStep = _snapshot.value.currentStep + 1
            )
            // Complete if verified
            _snapshot.value = _snapshot.value.copy(
                state = TaskState.COMPLETED
            )
            TaskState.COMPLETED
        } else {
            val nextAttempts = _snapshot.value.repairAttempts + 1
            if (nextAttempts > context.maxRepairAttempts) {
                // Exceeded bounded retry ceiling!
                _snapshot.value = _snapshot.value.copy(
                    state = TaskState.FAILED,
                    repairAttempts = nextAttempts,
                    failureReason = "Bounded repair limit reached (${context.maxRepairAttempts} attempts). Tests still failing."
                )
                TaskState.FAILED
            } else {
                // Enter Debugging loop
                _snapshot.value = _snapshot.value.copy(
                    state = TaskState.DEBUGGING,
                    repairAttempts = nextAttempts,
                    currentStep = _snapshot.value.currentStep + 1
                )
                TaskState.DEBUGGING
            }
        }
    }

    fun recordEvidence(kind: EvidenceKind, summary: String, exitCode: Int? = null, payloadPath: String? = null) {
        val record = EvidenceRecord(
            id = UUID.randomUUID().toString(),
            taskId = context.taskId,
            kind = kind,
            summary = summary,
            exitCode = exitCode,
            payloadPath = payloadPath,
            createdAt = System.currentTimeMillis()
        )
        _snapshot.value = _snapshot.value.copy(
            evidenceList = _snapshot.value.evidenceList + record
        )
    }
}

