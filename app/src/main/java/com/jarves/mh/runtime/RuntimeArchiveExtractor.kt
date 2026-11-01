package com.jarves.mh.runtime

import android.system.Os
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream

/**
 * Handles tar, zstd, and zip archive extraction, downloads, and checksum verification
 * for the host Ubuntu PRoot and guest agent runtimes.
 */
internal object RuntimeArchiveExtractor {

    fun safeChild(root: File, relative: String): File {
        val canonicalRoot = root.canonicalFile
        val target = File(root, relative).canonicalFile
        require(target.path == canonicalRoot.path || target.path.startsWith("${canonicalRoot.path}${File.separator}")) {
            "Archive path escapes destination: $relative"
        }
        return target
    }

    fun digest(file: File, algorithm: String = "SHA-256"): String {
        val digest = MessageDigest.getInstance(algorithm)
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun fetchText(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("Accept", "application/json")
        check(connection.responseCode in 200..299) { "Request failed with HTTP ${connection.responseCode}" }
        return connection.inputStream.bufferedReader().use { it.readText() }.also { connection.disconnect() }
    }

    suspend fun downloadVerified(
        url: String,
        destination: File,
        expectedChecksum: String,
        algorithm: String = "SHA-256",
        onBytes: suspend (downloaded: Long, total: Long) -> Unit,
    ) {
        destination.parentFile?.mkdirs()
        if (destination.isFile && digest(destination, algorithm).equals(expectedChecksum, ignoreCase = true)) {
            onBytes(destination.length(), destination.length())
            return
        }
        val temporary = File(destination.parentFile, "${destination.name}.part")
        var existing = temporary.takeIf(File::isFile)?.length() ?: 0L
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 120_000
        connection.instanceFollowRedirects = true
        if (existing > 0L) connection.setRequestProperty("Range", "bytes=$existing-")
        check(connection.responseCode in 200..299) { "Download failed with HTTP ${connection.responseCode}" }
        val resumed = connection.responseCode == HttpURLConnection.HTTP_PARTIAL && existing > 0L
        if (!resumed) {
            temporary.delete()
            existing = 0L
        }
        val total = connection.contentLengthLong.takeIf { it >= 0L }?.plus(existing) ?: -1L
        connection.inputStream.use { input ->
            FileOutputStream(temporary, resumed).use { output ->
                val buffer = ByteArray(128 * 1024)
                var downloaded = existing
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloaded += count
                    onBytes(downloaded, total)
                }
            }
        }
        connection.disconnect()
        val actual = digest(temporary, algorithm)
        if (!actual.equals(expectedChecksum, ignoreCase = true)) {
            temporary.delete()
            error("Downloaded file checksum did not match")
        }
        destination.delete()
        check(temporary.renameTo(destination)) { "Could not finish download" }
    }

    fun extractZipArchive(archive: File, destination: File) {
        ZipInputStream(BufferedInputStream(archive.inputStream())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.removePrefix("./").trimStart('/')
                if (name.isNotBlank()) {
                    val target = safeChild(destination, name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output -> zip.copyTo(output) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    fun extractRootfs(archive: File, destination: File) {
        val deferredLinks = mutableListOf<Pair<File, File>>()
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(archive.inputStream()))).use { tar ->
            var entry: TarArchiveEntry? = tar.nextEntry
            while (entry != null) {
                val cleanName = entry.name.removePrefix("./")
                val target = safeChild(destination, cleanName)
                when {
                    entry.isDirectory -> target.mkdirs()
                    entry.isSymbolicLink -> {
                        target.parentFile?.mkdirs()
                        if (target.exists() || java.nio.file.Files.isSymbolicLink(target.toPath())) target.delete()
                        Os.symlink(entry.linkName, target.absolutePath)
                    }
                    entry.isLink -> {
                        target.parentFile?.mkdirs()
                        val linkTarget = safeChild(destination, entry.linkName.removePrefix("./"))
                        if (linkTarget.exists()) {
                            linkTarget.inputStream().use { input -> FileOutputStream(target).use { input.copyTo(it) } }
                        } else {
                            deferredLinks += target to linkTarget
                        }
                    }
                    entry.isFile -> {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output -> tar.copyTo(output) }
                        runCatching { Os.chmod(target.absolutePath, entry.mode and 0b111111111) }
                    }
                }
                entry = tar.nextEntry
            }
        }
        deferredLinks.forEach { (target, linkTarget) ->
            require(linkTarget.isFile) { "Archive hard-link target is missing" }
            target.parentFile?.mkdirs()
            linkTarget.inputStream().use { input -> FileOutputStream(target).use { input.copyTo(it) } }
            runCatching { Os.chmod(target.absolutePath, Os.stat(linkTarget.absolutePath).st_mode) }
        }
    }

    fun extractZstdTar(archive: File, destination: File) {
        val deferredLinks = mutableListOf<Pair<File, File>>()
        TarArchiveInputStream(
            ZstdCompressorInputStream(BufferedInputStream(archive.inputStream())),
        ).use { tar ->
            var entry: TarArchiveEntry? = tar.nextEntry
            while (entry != null) {
                val cleanName = entry.name.removePrefix("./")
                val target = safeChild(destination, cleanName)
                when {
                    entry.isDirectory -> target.mkdirs()
                    entry.isSymbolicLink -> {
                        target.parentFile?.mkdirs()
                        if (target.exists() || java.nio.file.Files.isSymbolicLink(target.toPath())) target.delete()
                        Os.symlink(entry.linkName, target.absolutePath)
                    }
                    entry.isLink -> {
                        target.parentFile?.mkdirs()
                        val linkTarget = safeChild(destination, entry.linkName.removePrefix("./"))
                        if (linkTarget.exists()) {
                            linkTarget.inputStream().use { input -> FileOutputStream(target).use { input.copyTo(it) } }
                        } else {
                            deferredLinks += target to linkTarget
                        }
                    }
                    entry.isFile -> {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output -> tar.copyTo(output) }
                        runCatching { Os.chmod(target.absolutePath, entry.mode and 0b111111111) }
                    }
                }
                entry = tar.nextEntry
            }
        }
        deferredLinks.forEach { (target, linkTarget) ->
            require(linkTarget.isFile) { "Archive hard-link target is missing" }
            target.parentFile?.mkdirs()
            linkTarget.inputStream().use { input -> FileOutputStream(target).use { input.copyTo(it) } }
            runCatching { Os.chmod(target.absolutePath, Os.stat(linkTarget.absolutePath).st_mode) }
        }
    }

    fun extractNodeArchive(archive: File, destination: File) {
        TarArchiveInputStream(
            GzipCompressorInputStream(BufferedInputStream(archive.inputStream())),
        ).use { tar ->
            var entry: TarArchiveEntry? = tar.nextEntry
            while (entry != null) {
                val relative = entry.name.removePrefix("./")
                val trimmed = relative.substringAfter('/', "")
                if (trimmed.isNotBlank()) {
                    val target = safeChild(destination, trimmed)
                    when {
                        entry.isDirectory -> target.mkdirs()
                        entry.isSymbolicLink -> {
                            target.parentFile?.mkdirs()
                            if (target.exists() || java.nio.file.Files.isSymbolicLink(target.toPath())) target.delete()
                            Os.symlink(entry.linkName, target.absolutePath)
                        }
                        entry.isFile -> {
                            target.parentFile?.mkdirs()
                            FileOutputStream(target).use { output -> tar.copyTo(output) }
                            runCatching { Os.chmod(target.absolutePath, entry.mode and 0b111111111) }
                        }
                    }
                }
                entry = tar.nextEntry
            }
        }
    }
}

