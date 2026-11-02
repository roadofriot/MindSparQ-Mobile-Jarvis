package com.jarves.mh.data

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/**
 * Manages public, user-accessible storage for MindSparQ projects.
 *
 * All projects created by MindSparQ Harness are mirrored into the standard
 * "Documents/MindSparQ-Mobile-Jarvis" public directory so users can browse,
 * edit, copy, or export their code directly via any Android File Manager
 * (Samsung My Files, Google Files, Solid Explorer, etc.) and USB/MTP.
 */
object ProjectStorageManager {
    private const val TAG = "ProjectStorage"
    const val FOLDER_NAME = "MindSparQ-Mobile-Jarvis"

    /**
     * Returns the public directory where MindSparQ projects are stored:
     * `/storage/emulated/0/Documents/MindSparQ-Mobile-Jarvis/`
     */
    fun getPublicProjectsDir(context: Context): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(docs, FOLDER_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        // Fallback to app external files if Documents is unmounted
        return if (dir.exists() && dir.canWrite()) dir else File(context.getExternalFilesDir(null), FOLDER_NAME).apply { mkdirs() }
    }

    /**
     * Returns the dedicated Plans directory:
     * `/storage/emulated/0/Documents/MindSparQ-Mobile-Jarvis/Plans/<planId>/`
     */
    fun getPublicPlanDir(context: Context, planId: String): File {
        val cleanName = planId.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
        val plansBase = File(getPublicProjectsDir(context), "Plans").apply { mkdirs() }
        return File(plansBase, cleanName).apply { mkdirs() }
    }

    /**
     * Returns the public folder for a specific project:
     * `/storage/emulated/0/Documents/MindSparQ-Mobile-Jarvis/Projects/<projectName>/`
     */
    fun getProjectPublicDir(context: Context, projectName: String): File {
        val cleanName = projectName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
        val projectsBase = File(getPublicProjectsDir(context), "Projects").apply { mkdirs() }
        return File(projectsBase, cleanName).apply { mkdirs() }
    }

    /**
     * Writes architectural and requirements plan artifacts to the dedicated Plans storage.
     */
    fun writePlanArtifacts(
        context: Context,
        planId: String,
        prdContent: String,
        trdContent: String,
        architectureContent: String,
        decisionsContent: String = "",
    ): File {
        val planDir = getPublicPlanDir(context, planId)
        File(planDir, "PRD.md").writeText(prdContent)
        File(planDir, "TRD.md").writeText(trdContent)
        File(planDir, "ARCHITECTURE.md").writeText(architectureContent)
        if (decisionsContent.isNotBlank()) {
            File(planDir, "DECISIONS.md").writeText(decisionsContent)
        }
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(
                    File(planDir, "PRD.md").absolutePath,
                    File(planDir, "TRD.md").absolutePath,
                    File(planDir, "ARCHITECTURE.md").absolutePath,
                ),
                null,
                null
            )
        } catch (_: Exception) {}
        return planDir
    }

    /**
     * Synchronizes all files from an internal workspace into the public Documents folder.
     */
    fun syncWorkspaceToPublic(context: Context, projectName: String, internalWorkspace: File): File {
        val targetDir = getProjectPublicDir(context, projectName)
        try {
            if (internalWorkspace.exists() && internalWorkspace.isDirectory) {
                internalWorkspace.copyRecursively(targetDir, overwrite = true)
                // Collect all copied file paths for deep MediaScanner indexing
                val allPaths = mutableListOf<String>()
                allPaths.add(targetDir.absolutePath)
                targetDir.walkTopDown().take(200).forEach { file ->
                    allPaths.add(file.absolutePath)
                }
                MediaScannerConnection.scanFile(
                    context,
                    allPaths.toTypedArray(),
                    null
                ) { path, uri ->
                    Log.d(TAG, "Scanned $path for MediaScanner: $uri")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync project to public storage", e)
        }
        return targetDir
    }

    /**
     * Writes architectural artifacts (PRD, TRD, ARCHITECTURE) to both internal and public folders.
     */
    fun writeProjectArtifacts(
        context: Context,
        projectName: String,
        internalWorkspace: File,
        prdContent: String,
        trdContent: String,
        architectureContent: String,
        readmeContent: String? = null,
    ) {
        val publicDir = getProjectPublicDir(context, projectName)
        listOf(internalWorkspace, publicDir).forEach { dir ->
            dir.mkdirs()
            File(dir, "PRD.md").writeText(prdContent)
            File(dir, "TRD.md").writeText(trdContent)
            File(dir, "ARCHITECTURE.md").writeText(architectureContent)
            if (!readmeContent.isNullOrBlank()) {
                File(dir, "README.md").writeText(readmeContent)
            }
        }
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(
                    File(publicDir, "PRD.md").absolutePath,
                    File(publicDir, "TRD.md").absolutePath,
                    File(publicDir, "ARCHITECTURE.md").absolutePath,
                ),
                null,
                null
            )
        } catch (_: Exception) {}
    }

    /**
     * Opens the native Android File Manager directly to the project's folder.
     */
    fun openInFileManager(context: Context, projectName: String): Boolean {
        val publicDir = getProjectPublicDir(context, projectName)
        // 1. First attempt: Launch Samsung My Files directly if available on device
        try {
            val samsungIntent = Intent("samsung.myfiles.intent.action.LAUNCH_MY_FILES").apply {
                putExtra("samsung.myfiles.intent.extra.START_PATH", publicDir.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (samsungIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(samsungIntent)
                return true
            }
        } catch (_: Exception) {}

        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", publicDir)
        }.getOrNull()

        // 2. Second attempt: Standard Android Documents / Folder viewer intent with standard vnd.android.document/directory MIME
        try {
            val folderIntent = Intent(Intent.ACTION_VIEW).apply {
                if (uri != null) {
                    setDataAndType(uri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                } else {
                    data = Uri.fromFile(publicDir)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (folderIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(folderIntent)
                return true
            }
        } catch (_: Exception) {}

        // 3. Third attempt: resource/folder or generic file intent
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (uri != null) {
                setDataAndType(uri, "resource/folder")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                data = Uri.fromFile(publicDir)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            // Fallback: generic viewer chooser
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    if (uri != null) {
                        setDataAndType(uri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        data = Uri.fromFile(publicDir)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(fallbackIntent, "Open in File Manager").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}

