package com.nikolasguillen.questlog.core.domain.translation

import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import kotlinx.coroutines.flow.Flow

/**
 * Translates a game's description on-device.
 *
 * There is one implementation per platform in `:core:data`: Gemini Nano on Android
 * (`GameDescriptionTranslatorImpl`) and Apple's on-device model on iOS (`AppleLanguageModelTranslator`). This port
 * exists so Settings and the game detail screen share one definition of "can this device translate right now",
 * not so an engine can be swapped on a platform that already has one — do not add a second one speculatively.
 */
interface GameDescriptionTranslator {

    /**
     * The on-device model's status for the current user, folding in the device-language gate: a device
     * already running in English reports [TranslationModelStatus.UNSUPPORTED] without even asking the model.
     * Both call sites read this single method instead of re-deriving the same two conditions.
     */
    suspend fun modelStatus(): TranslationModelStatus

    /**
     * Translates [description] for game [gameId] into the device's language, or returns `null` when it
     * cannot — blank or too long input, an unsupported device, or a model failure. `null` is not an
     * error to surface: the caller's whole contract is "show the original text instead", so there is no
     * typed failure here the way `RepositoryError` models network failures.
     */
    suspend fun translate(gameId: Int, description: String): String?

    /** Downloads the on-device translation model, called only from an explicit user tap in Settings. */
    fun downloadModel(): Flow<TranslationModelDownload>
}
