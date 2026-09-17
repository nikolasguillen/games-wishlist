package com.example.gameswishlist.core.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
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

    /**
     * Triggers the system download of Gemini Nano, called only from an explicit user tap in Settings.
     * The total size arrives once, in [DownloadStatus.DownloadStarted]; every later
     * [DownloadStatus.DownloadProgress] reports a cumulative byte count against that same total, so it is
     * held in a local for the life of the collection rather than re-read from each emission. A failure
     * inside the underlying flow is mapped to [GeminiNanoDownload.Failed] rather than thrown, the same
     * no-throw contract [generate] and [status] already follow.
     */
    fun download(): Flow<GeminiNanoDownload> = flow {
        var bytesToDownload = 0L
        model.download().collect { downloadStatus ->
            when (downloadStatus) {
                is DownloadStatus.DownloadStarted -> bytesToDownload = downloadStatus.bytesToDownload
                is DownloadStatus.DownloadProgress -> {
                    val fraction = bytesToDownload.takeIf { it > 0 }?.let {
                        (downloadStatus.totalBytesDownloaded.toFloat() / it).coerceIn(0f, 1f)
                    }
                    emit(GeminiNanoDownload.Progress(fraction))
                }
                DownloadStatus.DownloadCompleted -> emit(GeminiNanoDownload.Completed)
                is DownloadStatus.DownloadFailed -> emit(GeminiNanoDownload.Failed)
            }
        }
    }.catch { e ->
        if (e is CancellationException) throw e
        emit(GeminiNanoDownload.Failed)
    }
}
