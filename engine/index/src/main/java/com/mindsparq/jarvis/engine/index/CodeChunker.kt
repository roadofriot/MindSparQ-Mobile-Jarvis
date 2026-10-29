package com.mindsparq.jarvis.engine.index

data class CodeChunk(
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val content: String,
    val symbolName: String? = null
)

object CodeChunker {

    private const val WINDOW_SIZE = 60
    private const val OVERLAP = 10

    fun chunkTextFile(filePath: String, fileContent: String): List<CodeChunk> {
        val lines = fileContent.lines()
        if (lines.isEmpty()) return emptyList()

        if (lines.size <= WINDOW_SIZE) {
            return listOf(
                CodeChunk(
                    filePath = filePath,
                    startLine = 1,
                    endLine = lines.size,
                    content = fileContent
                )
            )
        }

        val chunks = mutableListOf<CodeChunk>()
        var start = 0

        while (start < lines.size) {
            val end = minOf(start + WINDOW_SIZE, lines.size)
            val chunkLines = lines.subList(start, end)
            chunks.add(
                CodeChunk(
                    filePath = filePath,
                    startLine = start + 1,
                    endLine = end,
                    content = chunkLines.joinToString("\n")
                )
            )
            if (end == lines.size) break
            start += (WINDOW_SIZE - OVERLAP)
        }
        return chunks
    }
}

