package com.nikolasguillen.questlog.core.domain.usecase.translation

import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import com.nikolasguillen.questlog.core.domain.translation.TranslationSubject

/**
 * Use case to translate the description of a game, a character or a developer on-device. Returns `null` when
 * translation is unavailable.
 */
class TranslateDescriptionUseCase(
    private val translator: GameDescriptionTranslator
) {
    suspend operator fun invoke(subject: TranslationSubject, description: String): String? {
        return translator.translate(subject, description)
    }
}
