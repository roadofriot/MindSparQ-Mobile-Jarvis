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
     * Returns the public folder for a specific project.
     */
    fun getProjectPublicDir(context: Context, projectName: String): File {
        val cleanName = projectName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
        return File(getPublicProjectsDir(context), cleanName).apply { mkdirs() }
    }

    /**
     * Synchronizes all files from an internal workspace into the public Documents folder.
     */
    fun syncWorkspaceToPublic(context: Context, projectName: String, internalWorkspace: File): File {
        val targetDir = getProjectPublicDir(context, projectName)
        try {
            if (internalWorkspace.exists() && internalWorkspace.isDirectory) {
                internalWorkspace.copyRecursively(targetDir, overwrite = true)
                // Trigger MediaScanner so files immediately appear in File Manager apps
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetDir.absolutePath),
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
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.files", publicDir)
        }.getOrNull()

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
            // Fallback: generic viewer
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
                context.startActivity(fallbackIntent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}

