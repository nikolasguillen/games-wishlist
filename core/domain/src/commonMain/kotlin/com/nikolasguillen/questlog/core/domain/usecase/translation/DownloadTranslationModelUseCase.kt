package com.nikolasguillen.questlog.core.domain.usecase.translation

import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import kotlinx.coroutines.flow.Flow

/** Use case to download the on-device translation model. */
class DownloadTranslationModelUseCase(
    private val translator: GameDescriptionTranslator
) {
    operator fun invoke(): Flow<TranslationModelDownload> {
        return translator.downloadModel()
    }
}
