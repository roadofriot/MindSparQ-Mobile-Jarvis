package com.jarves.mh.runtime

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeArchiveExtractorTest {
    @Test
    fun extractionReplacesSelfReferentialSymlinkWithArchiveFile() = withTempDirectory { root ->
        val library = root.resolve("usr/lib/libexample.so.1.0")
        library.parentFile!!.mkdirs()
        Files.createSymbolicLink(library.toPath(), Paths.get(library.name))

        val archive = createGzipTar("usr/lib/libexample.so.1.0", "library contents")
        val archiveFile = root.resolve("overlay.tar.gz").apply { writeBytes(archive) }

        RuntimeArchiveExtractor.extractRootfs(archiveFile, root)

        assertFalse(Files.isSymbolicLink(library.toPath()))
        assertEquals("library contents", library.readText())
    }

    @Test
    fun safeChildRejectsPathsWhoseParentSymlinkEscapesDestination() = withTempDirectory { root ->
        val outside = Files.createTempDirectory("runtime-extractor-outside")
        try {
            Files.createSymbolicLink(root.resolve("outside").toPath(), outside)

            val failure = runCatching {
                RuntimeArchiveExtractor.safeChild(root, "outside/file")
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
        } finally {
            outside.toFile().deleteRecursively()
        }
    }

    private fun createGzipTar(path: String, contents: String): ByteArray {
        val output = ByteArrayOutputStream()
        GzipCompressorOutputStream(output).use { compressed ->
            TarArchiveOutputStream(compressed).use { tar ->
                val data = contents.toByteArray()
                tar.putArchiveEntry(TarArchiveEntry(path).apply { size = data.size.toLong() })
                tar.write(data)
                tar.closeArchiveEntry()
            }
        }
        return output.toByteArray()
    }

    private fun withTempDirectory(block: (File) -> Unit) {
        val root = Files.createTempDirectory("runtime-archive-extractor-test").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
