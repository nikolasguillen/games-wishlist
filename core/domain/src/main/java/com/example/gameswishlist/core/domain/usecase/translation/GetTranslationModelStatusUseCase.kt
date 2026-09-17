package com.example.gameswishlist.core.domain.usecase.translation

import com.example.gameswishlist.core.domain.translation.GameDescriptionTranslator
import com.example.gameswishlist.core.model.TranslationModelStatus
import javax.inject.Inject

/** Use case to read the on-device translation model's current status for this user. */
class GetTranslationModelStatusUseCase @Inject constructor(
    private val translator: GameDescriptionTranslator
) {
    suspend operator fun invoke(): TranslationModelStatus {
        return translator.modelStatus()
    }
}
