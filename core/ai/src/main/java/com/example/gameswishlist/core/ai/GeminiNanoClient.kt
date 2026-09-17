package com.example.gameswishlist.core.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around ML Kit's GenAI Prompt API, the only place in the app that talks to Gemini Nano.
 *
 * The client is process-scoped and never closed: [Generation.getClient] hands out a handle to the
 * on-device model shared by every app on the device, not a resource this app owns.
 */
@Singleton
class GeminiNanoClient @Inject constructor() {

    private val model: GenerativeModel by lazy { Generation.getClient() }

    /**
     * Gemini Nano's current availability on this device: never installed and never installable
     * ([GeminiNanoStatus.UNAVAILABLE]), installable but not yet requested ([GeminiNanoStatus.DOWNLOADABLE]),
     * mid-download ([GeminiNanoStatus.DOWNLOADING]), or ready to use ([GeminiNanoStatus.AVAILABLE]).
     * An unrecognized status and any exception both map to [GeminiNanoStatus.UNAVAILABLE].
     */
    suspend fun status(): GeminiNanoStatus = try {
        when (model.checkStatus()) {
            FeatureStatus.AVAILABLE -> GeminiNanoStatus.AVAILABLE
            FeatureStatus.DOWNLOADING -> GeminiNanoStatus.DOWNLOADING
            FeatureStatus.DOWNLOADABLE -> GeminiNanoStatus.DOWNLOADABLE
            else -> GeminiNanoStatus.UNAVAILABLE
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        GeminiNanoStatus.UNAVAILABLE
    }

    /**
     * Runs [prompt] through Gemini Nano, returning `null` on any failure. Callers treat `null` as
     * "translation unavailable right now", not as an error to surface — this client has no typed
     * failure the way the network layer maps exceptions to a `RepositoryError`.
     */
    suspend fun generate(prompt: String): String? = try {
        model.generateContent(prompt).candidates.firstOrNull()?.text
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
