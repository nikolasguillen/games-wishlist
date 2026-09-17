package com.example.gameswishlist.core.domain.usecase.translation

import com.example.gameswishlist.core.domain.translation.GameDescriptionTranslator
import com.example.gameswishlist.core.model.TranslationModelDownload
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use case to download the on-device translation model. */
class DownloadTranslationModelUseCase @Inject constructor(
    private val translator: GameDescriptionTranslator
) {
    operator fun invoke(): Flow<TranslationModelDownload> {
        return translator.downloadModel()
    }
}
