package com.nikolasguillen.questlog.core.domain.usecase.translation

import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import com.nikolasguillen.questlog.core.model.TranslationModelStatus

/** Use case to read the on-device translation model's current status for this user. */
class GetTranslationModelStatusUseCase(
    private val translator: GameDescriptionTranslator
) {
    suspend operator fun invoke(): TranslationModelStatus {
        return translator.modelStatus()
    }
}
