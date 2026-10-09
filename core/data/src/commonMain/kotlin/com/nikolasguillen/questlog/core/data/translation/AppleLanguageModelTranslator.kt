package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.database.dao.TranslationDao
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * [GameDescriptionTranslator] backed by Apple's on-device model through [AppleLanguageModelBridge]. Bound only on
 * iOS, but kept in common code so it runs in the JVM tests against a fake bridge.
 *
 * The app cannot download Apple's model: the system fetches it once Apple Intelligence is on. So
 * [modelStatus] never reports `DOWNLOADABLE`, and [downloadModel] only watches for the system to finish.
 */
internal class AppleLanguageModelTranslator(
    private val bridge: AppleLanguageModelBridge,
    private val translationDao: TranslationDao
) : GameDescriptionTranslator {

    override suspend fun modelStatus(): TranslationModelStatus {
        // Cheap check first, as on Android: no point asking the model about a language the user already reads.
        if (bridge.preferredLanguageTag().isEnglish()) return TranslationModelStatus.UNSUPPORTED
        return when (bridge.availability()) {
            AppleLanguageModelAvailability.AVAILABLE -> TranslationModelStatus.READY
            AppleLanguageModelAvailability.NOT_READY -> TranslationModelStatus.DOWNLOADING
            AppleLanguageModelAvailability.UNAVAILABLE,
            AppleLanguageModelAvailability.LANGUAGE_UNSUPPORTED -> TranslationModelStatus.UNSUPPORTED
        }
    }

    override suspend fun translate(gameId: Int, description: String): String? =
        translationDao.translateWithCache(gameId, bridge.preferredLanguageTag(), description) {
            bridge.generateText(
                instructions = buildTranslationInstructions(bridge.preferredLanguageEnglishName()),
                prompt = buildTranslationSource(description)
            )
        }

    /**
     * A cold flow, unlike Android's shared download job: there is no download here to keep alive, only a status
     * to watch, so it is fine for the collector's scope to cancel it.
     */
    override fun downloadModel(): Flow<TranslationModelDownload> = flow {
        emit(TranslationModelDownload.InProgress(fraction = null))
        while (true) {
            delay(STATUS_POLL_INTERVAL)
            when (bridge.availability()) {
                AppleLanguageModelAvailability.AVAILABLE -> {
                    emit(TranslationModelDownload.Completed)
                    return@flow
                }
                AppleLanguageModelAvailability.NOT_READY -> Unit
                AppleLanguageModelAvailability.UNAVAILABLE,
                AppleLanguageModelAvailability.LANGUAGE_UNSUPPORTED -> {
                    emit(TranslationModelDownload.Failed)
                    return@flow
                }
            }
        }
    }

    private suspend fun AppleLanguageModelBridge.generateText(instructions: String, prompt: String): String? =
        suspendCancellableCoroutine { continuation ->
            // A resume after cancellation is ignored by CancellableContinuation, so the Swift side may still
            // report its own cancellation as null without harm.
            val generation = generate(instructions, prompt) { result -> continuation.resume(result) }
            continuation.invokeOnCancellation { generation.cancel() }
        }

    private fun String.isEnglish(): Boolean = substringBefore('-').equals("en", ignoreCase = true)

    private companion object {
        // Coarse on purpose, as on Android: availability is a handful of states, not a byte count.
        val STATUS_POLL_INTERVAL = 15.seconds
    }
}
