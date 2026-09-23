package com.nikolasguillen.questlog.core.domain.usecase.translation

import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import javax.inject.Inject

/** Use case to translate a game's description on-device. Returns `null` when translation is unavailable. */
class TranslateGameDescriptionUseCase @Inject constructor(
    private val translator: GameDescriptionTranslator
) {
    suspend operator fun invoke(gameId: Int, description: String): String? {
        return translator.translate(gameId, description)
    }
}
