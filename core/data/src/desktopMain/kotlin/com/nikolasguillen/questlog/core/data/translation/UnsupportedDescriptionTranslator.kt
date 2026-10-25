package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import com.nikolasguillen.questlog.core.domain.translation.TranslationSubject
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Desktop has no on-device language model, so translation is never offered: the status is always
 * [TranslationModelStatus.UNSUPPORTED], which hides every translation control, and the original text is always shown.
 */
internal class UnsupportedDescriptionTranslator : GameDescriptionTranslator {

    override suspend fun modelStatus(): TranslationModelStatus = TranslationModelStatus.UNSUPPORTED

    override suspend fun translate(subject: TranslationSubject, description: String): String? = null

    /** Nothing can be downloaded; only reachable if a caller ignores [TranslationModelStatus.UNSUPPORTED]. */
    override fun downloadModel(): Flow<TranslationModelDownload> = flowOf(TranslationModelDownload.Failed)
}
