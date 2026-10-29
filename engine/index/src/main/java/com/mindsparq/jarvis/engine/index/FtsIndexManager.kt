package com.mindsparq.jarvis.engine.index

data class RetrievalResult(
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val snippet: String,
    val score: Float
)

class FtsIndexManager {

    private val indexedChunks = mutableListOf<CodeChunk>()

    fun indexFile(filePath: String, content: String) {
        // Remove existing chunks for this file
        indexedChunks.removeAll { it.filePath == filePath }
        // Add new chunks
        indexedChunks.addAll(CodeChunker.chunkTextFile(filePath, content))
    }

    fun search(query: String, topK: Int = 5): List<RetrievalResult> {
        val terms = query.lowercase().split(Regex("""\s+""")).filter { it.length > 2 }
        if (terms.isEmpty()) return emptyList()

        val scored = indexedChunks.mapNotNull { chunk ->
            val lowerContent = chunk.content.lowercase()
            var matches = 0
            for (term in terms) {
                if (lowerContent.contains(term)) matches++
            }
            if (matches > 0) {
                val score = matches.toFloat() / terms.size
                RetrievalResult(
                    filePath = chunk.filePath,
                    startLine = chunk.startLine,
                    endLine = chunk.endLine,
                    snippet = chunk.content,
                    score = score
                )
            } else {
                null
            }
        }.sortedByDescending { it.score }

        return scored.take(topK)
    }

    fun formatPromptContext(results: List<RetrievalResult>): String {
        if (results.isEmpty()) return ""
        return buildString {
            appendLine("### Relevant Codebase Context (RAG):")
            results.forEach { result ->
                appendLine("--- ${result.filePath}:${result.startLine}-${result.endLine} ---")
                appendLine(result.snippet)
                appendLine()
            }
        }
    }
}

