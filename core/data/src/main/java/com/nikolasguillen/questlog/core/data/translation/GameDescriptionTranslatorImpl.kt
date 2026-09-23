package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.ai.GeminiNanoClient
import com.nikolasguillen.questlog.core.ai.GeminiNanoDownload
import com.nikolasguillen.questlog.core.ai.GeminiNanoStatus
import com.nikolasguillen.questlog.core.data.mapper.toTranslationModelDownload
import com.nikolasguillen.questlog.core.data.mapper.toTranslationModelStatus
import com.nikolasguillen.questlog.core.data.translation.GameDescriptionTranslatorImpl.Companion.STATUS_POLL_INTERVAL
import com.nikolasguillen.questlog.core.database.dao.TranslationDao
import com.nikolasguillen.questlog.core.database.entity.TranslatedDescriptionEntity
import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class GameDescriptionTranslatorImpl @Inject constructor(
    private val geminiNanoClient: GeminiNanoClient,
    private val translationDao: TranslationDao,
    private val downloadScope: CoroutineScope
) : GameDescriptionTranslator {

    override suspend fun modelStatus(): TranslationModelStatus {
        // Cheap check first: no point asking AICore about a language the device is already showing.
        if (Locale.getDefault().language == Locale.ENGLISH.language) return TranslationModelStatus.UNSUPPORTED
        return geminiNanoClient.status().toTranslationModelStatus()
    }

    override suspend fun translate(gameId: Int, description: String): String? {
        if (description.isBlank() || description.length > MAX_TRANSLATABLE_CHARS) return null

        val languageTag = Locale.getDefault().toLanguageTag()
        val sourceHash = description.hashCode()

        val cached = translationDao.getTranslation(gameId, languageTag)
        if (cached != null && cached.sourceHash == sourceHash) {
            // Rows written before the prompt fix can still carry a leaked label; sanitize on read too.
            return cached.translatedText.stripTranslationArtifacts()
        }

        val translated = geminiNanoClient.generate(
            prefix = buildTranslationPromptPrefix(),
            suffix = buildTranslationPromptSuffix(description)
        )
        if (translated.isNullOrBlank()) return null

        val sanitized = translated.stripTranslationArtifacts()
        translationDao.saveTranslation(
            TranslatedDescriptionEntity(
                gameId = gameId,
                languageTag = languageTag,
                sourceHash = sourceHash,
                translatedText = sanitized
            )
        )
        return sanitized
    }

    // Held here rather than started fresh per call: GeminiNanoClient.download() is a cold Flow, and
    // cancelling its collector cancels the SDK's own download job along with it. Collecting it from
    // SettingsViewModel's scope would silently abort a real, in-flight download every time the user
    // navigates away from Settings, not just lose the UI's visibility into it — so the collection lives
    // in downloadScope, which outlives any single ViewModel, and every caller shares the one StateFlow.
    private val downloadState =
        MutableStateFlow<TranslationModelDownload>(TranslationModelDownload.InProgress(fraction = null))
    private var downloadJob: Job? = null

    override fun downloadModel(): Flow<TranslationModelDownload> {
        val job = downloadJob
        if (job == null || !job.isActive) {
            downloadJob = downloadScope.launch { runDownload() }
        }
        return downloadState
    }

    /**
     * Reaching here with no job already tracked means either a genuine fresh start (status
     * [GeminiNanoStatus.DOWNLOADABLE]) or a status inherited from a download that was running in a
     * previous, now-dead process. [GeminiNanoClient.download] excludes any feature that is not itself
     * [GeminiNanoStatus.DOWNLOADABLE] from the batch it (re)downloads, so calling it in the second case
     * resolves as an immediate, false [GeminiNanoDownload.Completed] instead of real progress — confirmed
     * on-device, and present even in Google's own AICoreModelHelper reference implementation
     * (google-ai-edge/gallery), which does not guard against it either. There is no supported way to
     * re-attach to that download's byte-level progress, so the inherited case falls through to
     * [pollInheritedDownload] instead of touching the SDK again.
     */
    private suspend fun runDownload() {
        if (geminiNanoClient.status() != GeminiNanoStatus.DOWNLOADABLE) {
            pollInheritedDownload()
            return
        }
        geminiNanoClient.download().collect { downloadState.value = it.toTranslationModelDownload() }
    }

    /**
     * Polls [GeminiNanoClient.status] for a download this process never started itself, so the row
     * still resolves to Ready on its own if the app was closed and reopened after the download finished
     * while it wasn't running — rather than sitting on the indeterminate state forever until the user
     * manually leaves and re-enters Settings. [STATUS_POLL_INTERVAL] is coarse on purpose: status is a
     * 4-value enum, not a byte count, so polling it every second buys nothing.
     */
    private suspend fun pollInheritedDownload() {
        downloadState.value = TranslationModelDownload.InProgress(fraction = null)
        while (true) {
            delay(STATUS_POLL_INTERVAL)
            when (geminiNanoClient.status()) {
                GeminiNanoStatus.AVAILABLE -> {
                    downloadState.value = TranslationModelDownload.Completed
                    return
                }
                // Still going: loop again without touching downloadState, already indeterminate.
                GeminiNanoStatus.DOWNLOADING -> Unit
                // Reverted rather than progressed — cancelled or failed on the system side.
                GeminiNanoStatus.DOWNLOADABLE, GeminiNanoStatus.UNAVAILABLE -> {
                    downloadState.value = TranslationModelDownload.Failed
                    return
                }
            }
        }
    }

    private companion object {
        // Well inside the Prompt API's ~4000-token ceiling; no IGDB summary comes close to this.
        const val MAX_TRANSLATABLE_CHARS = 8000
        val STATUS_POLL_INTERVAL = 15.seconds
    }
}
