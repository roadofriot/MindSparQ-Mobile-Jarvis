package com.mindsparq.jarvis.ai.gateway

import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Serializable
data class OfflineModelInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val quantization: String,
    val recommendedMinRamGb: Int,
    val downloadUrl: String,
    val isDownloaded: Boolean = false
)

class OfflineModelManager(
    private val baseStorageDir: File? = null
) {

    private val catalog = listOf(
        OfflineModelInfo(
            id = "qwen2.5-coder-0.5b-q4",
            name = "Qwen 2.5 Coder 0.5B (Ultralight)",
            sizeBytes = 398_000_000L,
            quantization = "Q4_K_M",
            recommendedMinRamGb = 3,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-0.5b-instruct-q4_k_m.gguf"
        ),
        OfflineModelInfo(
            id = "qwen2.5-coder-1.5b-q4",
            name = "Qwen 2.5 Coder 1.5B (Fast & Efficient)",
            sizeBytes = 1_050_000_000L,
            quantization = "Q4_K_M",
            recommendedMinRamGb = 6,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf"
        ),
        OfflineModelInfo(
            id = "deepseek-r1-distill-1.5b-q4",
            name = "DeepSeek R1 Distill Qwen 1.5B (Reasoning)",
            sizeBytes = 1_120_000_000L,
            quantization = "Q4_K_M",
            recommendedMinRamGb = 6,
            downloadUrl = "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf"
        ),
        OfflineModelInfo(
            id = "llama-3.2-3b-q4",
            name = "Llama 3.2 3B Instruct (High Capability)",
            sizeBytes = 1_950_000_000L,
            quantization = "Q4_K_M",
            recommendedMinRamGb = 10,
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
        )
    )

    private val downloadedModelIds = mutableSetOf<String>()

    init {
        baseStorageDir?.let { dir ->
            val modelsDir = File(dir, "models")
            if (modelsDir.exists() && modelsDir.isDirectory) {
                catalog.forEach { model ->
                    val file = File(modelsDir, "${model.id}.gguf")
                    if (file.exists() && file.length() > 1_000_000L) {
                        downloadedModelIds.add(model.id)
                    }
                }
            }
        }
    }

    fun getModelsForDevice(deviceRamGb: Int): List<OfflineModelInfo> {
        return catalog.map { it.copy(isDownloaded = downloadedModelIds.contains(it.id)) }
    }

    fun getRecommendedModel(deviceRamGb: Int): OfflineModelInfo {
        val candidates = catalog.filter { it.recommendedMinRamGb <= deviceRamGb }
        return candidates.maxByOrNull { it.recommendedMinRamGb } ?: catalog.first()
    }

    fun markDownloaded(modelId: String) {
        downloadedModelIds.add(modelId)
    }

    fun deleteModel(modelId: String) {
        downloadedModelIds.remove(modelId)
        baseStorageDir?.let { dir ->
            val target = File(File(dir, "models"), "$modelId.gguf")
            if (target.exists()) target.delete()
        }
    }

    fun isDownloaded(modelId: String): Boolean = downloadedModelIds.contains(modelId)

    fun getModelFile(modelId: String): File? {
        val dir = baseStorageDir ?: return null
        val target = File(File(dir, "models"), "$modelId.gguf")
        return if (target.exists() && target.length() > 0) target else null
    }

    suspend fun downloadModel(
        modelId: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val modelInfo = catalog.find { it.id == modelId }
            ?: return@withContext Result.failure(IllegalArgumentException("Unknown model: $modelId"))

        val targetDir = baseStorageDir?.let { File(it, "models") } ?: File("/tmp/mindsparq-models")
        if (!targetDir.exists()) targetDir.mkdirs()

        val destFile = File(targetDir, "${modelInfo.id}.gguf")
        val partFile = File(targetDir, "${modelInfo.id}.gguf.part")

        try {
            val url = URL(modelInfo.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.connect()

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: modelInfo.sizeBytes
            var bytesReadTotal = 0L

            connection.inputStream.use { input ->
                FileOutputStream(partFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        output.write(buffer, 0, count)
                        bytesReadTotal += count
                        onProgress(bytesReadTotal, totalBytes)
                    }
                }
            }

            if (partFile.renameTo(destFile)) {
                markDownloaded(modelId)
                Result.success(destFile)
            } else {
                Result.failure(IllegalStateException("Could not rename part file to ${destFile.name}"))
            }
        } catch (e: Exception) {
            if (partFile.exists()) partFile.delete()
            Result.failure(e)
        }
    }
}

class HybridRouteManager(
    private val offlineModelManager: OfflineModelManager
) {
    fun resolveRoute(isNetworkAvailable: Boolean, forceOffline: Boolean, deviceRamGb: Int): String {
        if (!isNetworkAvailable || forceOffline) {
            val recommended = offlineModelManager.getRecommendedModel(deviceRamGb)
            return "offline:${recommended.id}"
        }
        return "cloud:multi-key-pool"
    }
}

