package com.jarves.mh.runtime

import com.jarves.mh.model.ProviderProfile

/**
 * Backward-compatibility wrapper around consolidated [LocalFormatGateway].
 * Retained to prevent breaking binary and test callers.
 */
@Deprecated("Use LocalFormatGateway directly", ReplaceWith("LocalFormatGateway(profile, apiKey)"))
internal class OpenRouterRoutingGateway(
    profile: ProviderProfile,
    apiKey: String,
) : AutoCloseable {
    private val delegate = LocalFormatGateway(profile, apiKey)
    val url: String get() = delegate.url

    fun start(): OpenRouterRoutingGateway = apply {
        delegate.start()
    }

    override fun close() {
        delegate.close()
    }
}
