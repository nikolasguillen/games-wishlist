package com.example.gameswishlist.core.domain.translation

import com.example.gameswishlist.core.model.TranslationModelDownload
import com.example.gameswishlist.core.model.TranslationModelStatus
import kotlinx.coroutines.flow.Flow

/**
 * Translates a game's description on-device.
 *
 * There is a single implementation today, backed by Gemini Nano (`GameDescriptionTranslatorImpl` in
 * `:core:data`). This port exists so Settings and the game detail screen share one definition of
 * "can this device translate right now", not so a second engine can be swapped in — do not add one
 * speculatively.
 */
interface GameDescriptionTranslator {

    /**
     * The on-device model's status for the current user, folding in the device-language gate: a device
     * already running in English reports [TranslationModelStatus.UNSUPPORTED] without even asking Gemini
     * Nano. Both call sites read this single method instead of re-deriving the same two conditions.
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
