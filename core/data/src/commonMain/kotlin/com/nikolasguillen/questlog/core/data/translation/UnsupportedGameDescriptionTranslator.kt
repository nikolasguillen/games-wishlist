package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.domain.translation.GameDescriptionTranslator
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * [GameDescriptionTranslator] for a platform with no on-device model. `UNSUPPORTED` already hides the Settings row
 * and the Game detail action, so the other two methods are unreachable in practice; they answer as the Android
 * implementation does on a device that cannot translate.
 */
class UnsupportedGameDescriptionTranslator : GameDescriptionTranslator {
    override suspend fun modelStatus(): TranslationModelStatus = TranslationModelStatus.UNSUPPORTED

    override suspend fun translate(gameId: Int, description: String): String? = null

    override fun downloadModel(): Flow<TranslationModelDownload> = flowOf(TranslationModelDownload.Failed)
}
