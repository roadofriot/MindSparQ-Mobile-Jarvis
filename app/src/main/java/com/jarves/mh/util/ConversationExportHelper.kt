package com.jarves.mh.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ConversationExportHelper {

    fun copyToClipboard(context: Context, text: String, label: String = "JARVIS Live Conversation") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun getExportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun exportToMarkdown(
        context: Context,
        title: String,
        messages: List<Pair<String, String>>,
    ): File? = runCatching {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JARVIS_${title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}_$timestamp.md"
        val file = File(getExportDir(context), fileName)

        val sb = StringBuilder()
        sb.append("# $title\n\n")
        sb.append("**Exported from:** MindSparQ Jarvis Live Assistant  \n")
        sb.append("**Date:** ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}  \n\n")
        sb.append("---\n\n")

        for ((sender, text) in messages) {
            val badge = if (sender.equals("USER", ignoreCase = true)) "👤 **You**" else "🤖 **JARVIS**"
            sb.append("### $badge\n\n$text\n\n")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        shareFile(context, file, "text/markdown")
        file
    }.onFailure {
        Toast.makeText(context, "Failed to export Markdown: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
    }.getOrNull()

    fun exportToDoc(
        context: Context,
        title: String,
        messages: List<Pair<String, String>>,
    ): File? = runCatching {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JARVIS_${title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}_$timestamp.doc"
        val file = File(getExportDir(context), fileName)

        val sb = StringBuilder()
        sb.append("===================================================\n")
        sb.append(" $title\n")
        sb.append(" MindSparQ Jarvis Live Engineering Assistant\n")
        sb.append(" Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("===================================================\n\n")

        for ((sender, text) in messages) {
            val label = if (sender.equals("USER", ignoreCase = true)) "[YOU]" else "[JARVIS]"
            sb.append("$label:\n$text\n\n---------------------------------------------------\n\n")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        shareFile(context, file, "application/msword")
        file
    }.onFailure {
        Toast.makeText(context, "Failed to export Doc: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
    }.getOrNull()

    fun exportToPdf(
        context: Context,
        title: String,
        messages: List<Pair<String, String>>,
    ): File? = runCatching {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JARVIS_${title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")}_$timestamp.pdf"
        val file = File(getExportDir(context), fileName)

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val margin = 40f
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = Paint().apply {
            color = Color.rgb(249, 115, 22) // PocketOrange
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val senderPaint = Paint().apply {
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin + 20f

        // Draw Header
        canvas.drawText(title, margin, y, titlePaint)
        y += 18f
        val dateStr = "Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())} · MindSparQ Jarvis"
        canvas.drawText(dateStr, margin, y, metaPaint)
        y += 16f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 24f

        fun splitTextToLines(text: String, paint: Paint, maxWidth: Float): List<String> {
            val result = mutableListOf<String>()
            val paragraphs = text.split("\n")
            for (para in paragraphs) {
                if (para.isBlank()) {
                    result.add("")
                    continue
                }
                val words = para.split(" ")
                var currentLine = StringBuilder()
                for (word in words) {
                    val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine.append(if (currentLine.isEmpty()) word else " $word")
                    } else {
                        if (currentLine.isNotEmpty()) result.add(currentLine.toString())
                        currentLine = StringBuilder(word)
                    }
                }
                if (currentLine.isNotEmpty()) result.add(currentLine.toString())
            }
            return result
        }

        for ((sender, text) in messages) {
            val isUser = sender.equals("USER", ignoreCase = true)
            senderPaint.color = if (isUser) Color.rgb(37, 99, 235) else Color.rgb(217, 119, 6)
            val senderLabel = if (isUser) "YOU" else "JARVIS"

            // Estimate space needed
            val bodyLines = splitTextToLines(text, textPaint, contentWidth)
            val totalNeeded = 22f + (bodyLines.size * 15f) + 16f

            if (y + totalNeeded > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin + 20f
            }

            canvas.drawText(senderLabel, margin, y, senderPaint)
            y += 15f

            for (line in bodyLines) {
                if (y > pageHeight - margin - 20f) {
                    document.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin + 20f
                }
                canvas.drawText(line, margin, y, textPaint)
                y += 14f
            }
            y += 12f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 16f
        }

        document.finishPage(page)

        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        shareFile(context, file, "application/pdf")
        file
    }.onFailure {
        Toast.makeText(context, "Failed to export PDF: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
    }.getOrNull()

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share / Export Conversation")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

