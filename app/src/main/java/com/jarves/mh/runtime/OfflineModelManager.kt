package com.jarves.mh.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class DeviceHardwareProfile(
    val manufacturer: String,
    val model: String,
    val totalRamGb: Float,
    val availRamGb: Float,
    val cpuCores: Int,
    val primaryAbi: String,
    val freeStorageGb: Float,
    val recommendedModelId: String,
    val recommendationReason: String,
)

data class OfflineModelSpec(
    val id: String,
    val name: String,
    val parameterSize: String,
    val formattedSize: String,
    val fileSizeBytes: Long,
    val quantization: String,
    val directUrl: String,
    val fileName: String,
    val description: String,
    val minRamGb: Float,
    val recommendedRamGb: Float,
)

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    DOWNLOADED,
    ERROR,
}

data class ModelDownloadState(
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
    val speedMbps: Float = 0f,
    val errorMessage: String? = null,
)

data class CacheCleanResult(
    val bytesFreed: Long,
    val formattedFreed: String,
    val remainingStorageGb: Float,
)

object OfflineModelManager {

    val AVAILABLE_MODELS: List<OfflineModelSpec> = listOf(
        OfflineModelSpec(
            id = "llama-3.2-3b",
            name = "Llama 3.2 3B Instruct",
            parameterSize = "3B",
            formattedSize = "2.02 GB",
            fileSizeBytes = 2_020_000_000L,
            quantization = "Q4_K_M",
            directUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            fileName = "llama-3.2-3b-instruct-q4_k_m.gguf",
            description = "High-tier autonomous coding, multi-file awareness, and deep reasoning.",
            minRamGb = 8.0f,
            recommendedRamGb = 10.0f,
        ),
        OfflineModelSpec(
            id = "deepseek-r1-1.5b",
            name = "DeepSeek R1 Distill 1.5B",
            parameterSize = "1.5B",
            formattedSize = "1.12 GB",
            fileSizeBytes = 1_121_000_000L,
            quantization = "Q4_K_M",
            directUrl = "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            fileName = "deepseek-r1-distill-qwen-1.5b-q4_k_m.gguf",
            description = "Offline algorithmic logic, chain-of-thought analysis, and math troubleshooting.",
            minRamGb = 4.0f,
            recommendedRamGb = 6.0f,
        ),
        OfflineModelSpec(
            id = "qwen2.5-coder-1.5b",
            name = "Qwen 2.5 Coder 1.5B",
            parameterSize = "1.5B",
            formattedSize = "1.05 GB",
            fileSizeBytes = 1_052_606_464L,
            quantization = "Q4_K_M",
            directUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-coder-1.5b-instruct-q4_k_m.gguf",
            description = "Specialized code synthesis, refactoring, and AST analysis with rapid token throughput.",
            minRamGb = 4.0f,
            recommendedRamGb = 6.0f,
        ),
        OfflineModelSpec(
            id = "qwen2.5-coder-0.5b",
            name = "Qwen 2.5 Coder 0.5B",
            parameterSize = "0.5B",
            formattedSize = "398 MB",
            fileSizeBytes = 397_750_272L,
            quantization = "Q4_K_M",
            directUrl = "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct-GGUF/resolve/main/qwen2.5-coder-0.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-coder-0.5b-instruct-q4_k_m.gguf",
            description = "Ultralight code companion. Instant loads on low-RAM hardware.",
            minRamGb = 2.0f,
            recommendedRamGb = 4.0f,
        ),
        OfflineModelSpec(
            id = "smollm2-360m",
            name = "SmolLM2 360M Instruct",
            parameterSize = "360M",
            formattedSize = "229 MB",
            fileSizeBytes = 229_000_000L,
            quantization = "Q4_K_M",
            directUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
            fileName = "smollm2-360m-instruct-q4_k_m.gguf",
            description = "Hyper-compact offline assistant with ultra-low power consumption.",
            minRamGb = 1.5f,
            recommendedRamGb = 2.5f,
        ),
    )

    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val _modelStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val modelStates: StateFlow<Map<String, ModelDownloadState>> = _modelStates.asStateFlow()

    private var activeModelId: String? = null

    fun getActiveModelId(): String? = activeModelId

    fun setActiveModelId(modelId: String?) {
        activeModelId = modelId
    }

    fun detectHardware(context: Context): DeviceHardwareProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRamGb = memInfo.totalMem.toFloat() / (1024f * 1024f * 1024f)
        val availRamGb = memInfo.availMem.toFloat() / (1024f * 1024f * 1024f)
        val cores = Runtime.getRuntime().availableProcessors()
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"

        val statFs = StatFs(context.filesDir.path)
        val freeStorageGb = (statFs.availableBlocksLong * statFs.blockSizeLong).toFloat() / (1024f * 1024f * 1024f)

        val (recId, recReason) = when {
            totalRamGb >= 9.5f -> Pair(
                "llama-3.2-3b",
                "Optimized for your ${String.format("%.1f", totalRamGb)} GB RAM: 3B parameters gives complete autonomous coding & deep context."
            )
            totalRamGb >= 5.5f -> Pair(
                "qwen2.5-coder-1.5b",
                "Recommended for your ${String.format("%.1f", totalRamGb)} GB RAM: 1.5B parameters ensures fast synthesis without memory pressure."
            )
            totalRamGb >= 3.5f -> Pair(
                "deepseek-r1-1.5b",
                "Best fit for ${String.format("%.1f", totalRamGb)} GB RAM: High-efficiency distilled reasoning model."
            )
            else -> Pair(
                "qwen2.5-coder-0.5b",
                "Ultralight model for sub-4 GB RAM: Zero memory stutter and low battery consumption."
            )
        }

        return DeviceHardwareProfile(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            totalRamGb = totalRamGb,
            availRamGb = availRamGb,
            cpuCores = cores,
            primaryAbi = primaryAbi,
            freeStorageGb = freeStorageGb,
            recommendedModelId = recId,
            recommendationReason = recReason,
        )
    }

    fun getModelsDirectory(context: Context): File {
        val externalModels = context.getExternalFilesDir("models")
        val dir = if (externalModels != null && externalModels.canWrite()) {
            File(externalModels, "gguf")
        } else {
            File(context.filesDir, "models/gguf")
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getModelFile(context: Context, model: OfflineModelSpec): File {
        return File(getModelsDirectory(context), model.fileName)
    }

    fun getPartFile(context: Context, model: OfflineModelSpec): File {
        return File(getModelsDirectory(context), "${model.fileName}.part")
    }

    fun syncModelStatuses(context: Context) {
        val current = _modelStates.value.toMutableMap()
        for (model in AVAILABLE_MODELS) {
            val file = getModelFile(context, model)
            val part = getPartFile(context, model)
            if (file.isFile && file.length() >= (model.fileSizeBytes * 0.90).toLong()) {
                current[model.id] = ModelDownloadState(
                    status = DownloadStatus.DOWNLOADED,
                    bytesDownloaded = file.length(),
                    totalBytes = file.length(),
                    progress = 1.0f,
                )
            } else if (downloadJobs[model.id]?.isActive == true) {
                // Keep active state
            } else if (part.isFile && part.length() > 0L) {
                val downloaded = part.length()
                val total = model.fileSizeBytes
                current[model.id] = ModelDownloadState(
                    status = DownloadStatus.PAUSED,
                    bytesDownloaded = downloaded,
                    totalBytes = total,
                    progress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 0.99f),
                )
            } else {
                current[model.id] = ModelDownloadState(
                    status = DownloadStatus.NOT_DOWNLOADED,
                    bytesDownloaded = 0L,
                    totalBytes = model.fileSizeBytes,
                    progress = 0f,
                )
            }
        }
        _modelStates.value = current
    }

    suspend fun startDownload(context: Context, model: OfflineModelSpec) = withContext(Dispatchers.IO) {
        val destination = getModelFile(context, model)
        val temporary = getPartFile(context, model)

        var existing = if (temporary.isFile) temporary.length() else 0L

        updateState(model.id) {
            it.copy(
                status = DownloadStatus.DOWNLOADING,
                bytesDownloaded = existing,
                totalBytes = model.fileSizeBytes,
                progress = (existing.toFloat() / model.fileSizeBytes.toFloat()).coerceIn(0f, 0.99f),
                speedMbps = 0f,
                errorMessage = null,
            )
        }

        try {
            var urlString = model.directUrl
            var connection: HttpURLConnection
            var redirects = 0
            while (true) {
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "MindSparQ-Harness-SLM/2.0")
                    if (existing > 0L) {
                        setRequestProperty("Range", "bytes=$existing-")
                    }
                }
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (location != null && redirects < 5) {
                        urlString = location
                        redirects++
                        continue
                    }
                }
                break
            }

            val responseCode = connection.responseCode
            val resumed = responseCode == HttpURLConnection.HTTP_PARTIAL && existing > 0L
            if (!resumed && responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == 416) {
                    // Range not satisfiable: temporary file might be complete
                    temporary.renameTo(destination)
                    updateState(model.id) {
                        it.copy(status = DownloadStatus.DOWNLOADED, progress = 1f, speedMbps = 0f)
                    }
                    return@withContext
                }
                throw RuntimeException("Download failed with HTTP code $responseCode")
            }

            if (!resumed) {
                temporary.delete()
                existing = 0L
            }

            val total = connection.contentLengthLong.takeIf { it > 0L }?.plus(existing) ?: model.fileSizeBytes
            var downloaded = existing
            var lastTime = System.currentTimeMillis()
            var bytesSinceLast = 0L

            connection.inputStream.use { input ->
                FileOutputStream(temporary, resumed).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceLast += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTime
                        if (elapsed >= 500) {
                            val speedBytesPerSec = (bytesSinceLast.toFloat() / (elapsed / 1000f))
                            val speedMbps = (speedBytesPerSec * 8f) / (1024f * 1024f)
                            lastTime = now
                            bytesSinceLast = 0L

                            updateState(model.id) {
                                it.copy(
                                    status = DownloadStatus.DOWNLOADING,
                                    bytesDownloaded = downloaded,
                                    totalBytes = total,
                                    progress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 0.99f),
                                    speedMbps = speedMbps,
                                )
                            }
                        }
                    }
                }
            }
            connection.disconnect()

            if (destination.exists()) destination.delete()
            if (temporary.renameTo(destination)) {
                updateState(model.id) {
                    it.copy(
                        status = DownloadStatus.DOWNLOADED,
                        bytesDownloaded = downloaded,
                        totalBytes = downloaded,
                        progress = 1.0f,
                        speedMbps = 0f,
                    )
                }
                if (activeModelId == null) {
                    activeModelId = model.id
                }
            } else {
                throw RuntimeException("Could not finalize model file.")
            }
        } catch (e: CancellationException) {
            updateState(model.id) {
                it.copy(status = DownloadStatus.PAUSED, speedMbps = 0f)
            }
        } catch (e: Exception) {
            updateState(model.id) {
                it.copy(
                    status = DownloadStatus.ERROR,
                    errorMessage = e.localizedMessage ?: "Download failed",
                    speedMbps = 0f,
                )
            }
        } finally {
            downloadJobs.remove(model.id)
        }
    }

    fun pauseDownload(modelId: String) {
        downloadJobs[modelId]?.cancel()
        downloadJobs.remove(modelId)
        updateState(modelId) {
            it.copy(status = DownloadStatus.PAUSED, speedMbps = 0f)
        }
    }

    fun registerJob(modelId: String, job: Job) {
        downloadJobs[modelId] = job
    }

    fun deleteModel(context: Context, model: OfflineModelSpec): Boolean {
        pauseDownload(model.id)
        val file = getModelFile(context, model)
        val part = getPartFile(context, model)
        var deleted = false
        if (file.exists()) deleted = file.delete() || deleted
        if (part.exists()) deleted = part.delete() || deleted
        if (activeModelId == model.id) {
            activeModelId = null
        }
        updateState(model.id) {
            ModelDownloadState(
                status = DownloadStatus.NOT_DOWNLOADED,
                bytesDownloaded = 0L,
                totalBytes = model.fileSizeBytes,
                progress = 0f,
            )
        }
        return deleted
    }

    fun clearAppCache(context: Context): CacheCleanResult {
        var bytesFreed = 0L

        fun deleteRecursive(file: File?): Long {
            if (file == null || !file.exists()) return 0L
            var freed = 0L
            if (file.isDirectory) {
                file.listFiles()?.forEach { child ->
                    freed += deleteRecursive(child)
                }
            }
            val len = file.length()
            if (file.delete()) {
                freed += len
            }
            return freed
        }

        bytesFreed += deleteRecursive(context.cacheDir)
        bytesFreed += deleteRecursive(context.codeCacheDir)
        context.externalCacheDir?.let { bytesFreed += deleteRecursive(it) }

        // Clean partial download files that might be abandoned
        val modelsDir = getModelsDirectory(context)
        modelsDir.listFiles()?.forEach { f ->
            if (f.name.endsWith(".part") && downloadJobs.isEmpty()) {
                val len = f.length()
                if (f.delete()) bytesFreed += len
            }
        }

        // Clean PRoot /tmp if available
        val prootTmp = File(context.filesDir, "rootfs/tmp")
        if (prootTmp.exists()) {
            bytesFreed += deleteRecursive(prootTmp)
        }

        // Hint garbage collector
        System.gc()

        val statFs = StatFs(context.filesDir.path)
        val freeStorageGb = (statFs.availableBlocksLong * statFs.blockSizeLong).toFloat() / (1024f * 1024f * 1024f)

        val formatted = if (bytesFreed >= 1024 * 1024 * 1024) {
            String.format("%.2f GB", bytesFreed.toFloat() / (1024f * 1024f * 1024f))
        } else {
            String.format("%.1f MB", bytesFreed.toFloat() / (1024f * 1024f))
        }

        syncModelStatuses(context)

        return CacheCleanResult(
            bytesFreed = bytesFreed,
            formattedFreed = formatted,
            remainingStorageGb = freeStorageGb,
        )
    }

    private fun updateState(modelId: String, transform: (ModelDownloadState) -> ModelDownloadState) {
        val current = _modelStates.value.toMutableMap()
        val old = current[modelId] ?: ModelDownloadState()
        current[modelId] = transform(old)
        _modelStates.value = current
    }
}

