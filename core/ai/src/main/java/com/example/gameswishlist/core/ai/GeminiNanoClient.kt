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
     * `true` only when Gemini Nano is already on the device and ready to use.
     *
     * [FeatureStatus.DOWNLOADABLE] and [FeatureStatus.DOWNLOADING] both count as *not* ready on
     * purpose: this app must never trigger the system download of a shared, multi-hundred-MB model on
     * the user's behalf, so [GenerativeModel.download] is intentionally never called anywhere in this
     * client. A device that could get Gemini Nano but does not have it yet is, for this feature,
     * indistinguishable from a device that never could.
     */
    suspend fun isModelReady(): Boolean = try {
        model.checkStatus() == FeatureStatus.AVAILABLE
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        false
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
