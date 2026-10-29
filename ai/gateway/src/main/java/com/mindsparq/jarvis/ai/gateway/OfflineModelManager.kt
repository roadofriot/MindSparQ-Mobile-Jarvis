package com.mindsparq.jarvis.ai.gateway

import kotlinx.serialization.Serializable

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

class OfflineModelManager {

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
    }

    fun isDownloaded(modelId: String): Boolean = downloadedModelIds.contains(modelId)
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

