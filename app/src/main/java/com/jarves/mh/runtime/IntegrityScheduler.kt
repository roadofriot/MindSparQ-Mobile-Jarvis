package com.jarves.mh.runtime

import android.content.Context
import com.jarves.mh.model.ChangeItem
import com.jarves.mh.model.DiffLine
import com.jarves.mh.model.DiffLineType
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Periodic workspace integrity monitor.
 *
 * Takes SHA-256 snapshots of each project workspace at configurable intervals and
 * compares them against the previous snapshot to detect unexpected modifications.
 * Runs entirely offline — no cloud calls, no network required. Results are kept
 * locally so the user can verify integrity even if cloud services go down.
 *
 * Designed for low-resource devices: checks one project at a time, bounds snapshot
 * size via [WorkspaceCheckpoints], and only re-snaps when the interval elapses.
 */
class IntegrityScheduler(
    private val context: Context,
    private val checkpoints: WorkspaceCheckpoints,
) {

    /** Result of a single integrity check. */
    data class IntegrityResult(
        val projectId: String,
        val projectName: String,
        val checkedAtMillis: Long = System.currentTimeMillis(),
        /** SHA-256 digest map of the current workspace state. */
        val snapshot: Map<String, String> = emptyMap(),
        /** Files that changed since the last snapshot. */
        val changedFiles: List<String> = emptyList(),
        /** Overall status. */
        val status: Status = Status.OK,
    ) {
        enum class Status { OK, CHANGED, ERROR, FIRST_CHECK }
    }

    private val lastSnapshots = ConcurrentHashMap<String, Map<String, String>>()
    private val lastResults = ConcurrentHashMap<String, IntegrityResult>()
    private val running = AtomicBoolean(false)
    private var checkIntervalMillis: Long = 30 * 60 * 1000L // default 30 minutes

    fun setCheckInterval(minutes: Long) {
        checkIntervalMillis = minutes * 60 * 1000L
    }

    fun getCheckIntervalMinutes(): Long = checkIntervalMillis / (60 * 1000)

    /** Returns the most recent result for a project, or null if never checked. */
    fun lastResult(projectId: String): IntegrityResult? = lastResults[projectId]

    /** Returns all results. */
    fun allResults(): List<IntegrityResult> = lastResults.values.toList()

    /**
     * Runs an integrity check for the given project now. Compares the current
     * workspace against the stored baseline snapshot.
     */
    suspend fun checkNow(projectId: String, projectName: String, workspace: File): IntegrityResult {
        return withContext(Dispatchers.IO) {
            try {
                val current = checkpoints.snapshot(workspace)
                val previous = lastSnapshots[projectId]

                val status = when {
                    previous == null -> IntegrityResult.Status.FIRST_CHECK
                    current == previous -> IntegrityResult.Status.OK
                    else -> IntegrityResult.Status.CHANGED
                }

                val changed = if (previous == null) emptyList() else {
                    checkpoints.changedFiles(workspace, previous)
                }

                val result = IntegrityResult(
                    projectId = projectId,
                    projectName = projectName,
                    checkedAtMillis = System.currentTimeMillis(),
                    snapshot = current,
                    changedFiles = changed,
                    status = status,
                )
                lastSnapshots[projectId] = current
                lastResults[projectId] = result
                result
            } catch (e: Exception) {
                val result = IntegrityResult(
                    projectId = projectId,
                    projectName = projectName,
                    checkedAtMillis = System.currentTimeMillis(),
                    status = IntegrityResult.Status.ERROR,
                )
                lastResults[projectId] = result
                result
            }
        }
    }

    /**
     * Runs integrity checks for all provided projects. Returns the list of results.
     */
    suspend fun checkAll(projects: List<Triple<String, String, File>>): List<IntegrityResult> {
        if (running.get()) return allResults()
        running.set(true)
        try {
            return projects.map { (id, name, workspace) ->
                checkNow(id, name, workspace)
            }
        } finally {
            running.set(false)
        }
    }

    /** Clears the stored baseline for a project so the next check is a first-check. */
    fun resetBaseline(projectId: String) {
        lastSnapshots.remove(projectId)
        lastResults.remove(projectId)
    }
}
