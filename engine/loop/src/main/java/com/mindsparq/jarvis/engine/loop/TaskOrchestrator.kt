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

class TaskOrchestrator(
    private val context: TaskContext,
    initialSnapshot: TaskSnapshot? = null,
    private val onTransition: ((TaskSnapshot) -> Unit)? = null
) {

    private val _snapshot = MutableStateFlow(
        initialSnapshot ?: TaskSnapshot(
            taskId = context.taskId,
            state = TaskState.QUEUED,
            currentStep = 0,
            repairAttempts = 0,
            evidenceList = emptyList()
        )
    )
    val snapshot: StateFlow<TaskSnapshot> = _snapshot.asStateFlow()

    private fun updateSnapshot(transform: (TaskSnapshot) -> TaskSnapshot) {
        val next = transform(_snapshot.value)
        _snapshot.value = next
        onTransition?.invoke(next)
    }

    fun startInspection(): Boolean {
        if (_snapshot.value.state != TaskState.QUEUED) return false
        updateSnapshot {
            it.copy(
                state = TaskState.INSPECTING,
                currentStep = it.currentStep + 1
            )
        }
        return true
    }

    fun proceedToPlanning(): Boolean {
        if (_snapshot.value.state != TaskState.INSPECTING) return false
        updateSnapshot {
            it.copy(
                state = TaskState.PLANNING,
                currentStep = it.currentStep + 1
            )
        }
        return true
    }

    fun requestApproval(diffSummary: String): Boolean {
        if (_snapshot.value.state != TaskState.PLANNING && _snapshot.value.state != TaskState.IMPLEMENTING) return false
        recordEvidence(EvidenceKind.DIFF, diffSummary)
        updateSnapshot {
            it.copy(
                state = TaskState.AWAITING_APPROVAL,
                currentStep = it.currentStep + 1
            )
        }
        return true
    }

    fun onApprovalGranted(): Boolean {
        if (_snapshot.value.state != TaskState.AWAITING_APPROVAL) return false
        recordEvidence(EvidenceKind.APPROVAL, "User approved implementation plan")
        updateSnapshot {
            it.copy(
                state = TaskState.IMPLEMENTING,
                currentStep = it.currentStep + 1
            )
        }
        return true
    }

    fun onApprovalDenied(reason: String): Boolean {
        if (_snapshot.value.state != TaskState.AWAITING_APPROVAL) return false
        recordEvidence(EvidenceKind.APPROVAL, "User denied action: $reason")
        updateSnapshot {
            it.copy(
                state = TaskState.CANCELLED,
                failureReason = "User rejected approval: $reason"
            )
        }
        return true
    }

    fun runTests(): Boolean {
        if (_snapshot.value.state != TaskState.IMPLEMENTING && _snapshot.value.state != TaskState.DEBUGGING) return false
        updateSnapshot {
            it.copy(
                state = TaskState.TESTING,
                currentStep = it.currentStep + 1
            )
        }
        return true
    }

    fun onTestsResult(allPassed: Boolean, testOutputSummary: String, exitCode: Int): TaskState {
        if (_snapshot.value.state != TaskState.TESTING) return _snapshot.value.state

        recordEvidence(EvidenceKind.TEST_RUN, testOutputSummary, exitCode = exitCode)

        return if (allPassed) {
            updateSnapshot {
                it.copy(
                    state = TaskState.VERIFYING,
                    currentStep = it.currentStep + 1
                )
            }
            // Complete if verified
            updateSnapshot {
                it.copy(
                    state = TaskState.COMPLETED
                )
            }
            TaskState.COMPLETED
        } else {
            val nextAttempts = _snapshot.value.repairAttempts + 1
            if (nextAttempts > context.maxRepairAttempts) {
                // Exceeded bounded retry ceiling!
                updateSnapshot {
                    it.copy(
                        state = TaskState.FAILED,
                        repairAttempts = nextAttempts,
                        failureReason = "Bounded repair limit reached (${context.maxRepairAttempts} attempts). Tests still failing."
                    )
                }
                TaskState.FAILED
            } else {
                // Enter Debugging loop
                updateSnapshot {
                    it.copy(
                        state = TaskState.DEBUGGING,
                        repairAttempts = nextAttempts,
                        currentStep = it.currentStep + 1
                    )
                }
                TaskState.DEBUGGING
            }
        }
    }

    fun interrupt(reason: String): Boolean {
        if (_snapshot.value.state == TaskState.COMPLETED || _snapshot.value.state == TaskState.FAILED || _snapshot.value.state == TaskState.CANCELLED) return false
        updateSnapshot {
            it.copy(
                state = TaskState.BLOCKED,
                failureReason = reason
            )
        }
        return true
    }

    fun resume(): Boolean {
        if (_snapshot.value.state != TaskState.BLOCKED) return false
        val resumeState = if (_snapshot.value.currentStep <= 1) TaskState.PLANNING else TaskState.IMPLEMENTING
        updateSnapshot {
            it.copy(
                state = resumeState,
                failureReason = null,
                currentStep = it.currentStep + 1
            )
        }
        return true
    }

    fun complete(report: String? = null): Boolean {
        updateSnapshot {
            it.copy(
                state = TaskState.COMPLETED,
                failureReason = null
            )
        }
        if (!report.isNullOrBlank()) {
            recordEvidence(EvidenceKind.APPROVAL, report)
        }
        return true
    }

    fun fail(reason: String): Boolean {
        updateSnapshot {
            it.copy(
                state = TaskState.FAILED,
                failureReason = reason
            )
        }
        return true
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
        updateSnapshot {
            it.copy(
                evidenceList = it.evidenceList + record
            )
        }
    }
}

