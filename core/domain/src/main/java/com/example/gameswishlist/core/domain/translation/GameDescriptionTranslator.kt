package com.example.gameswishlist.core.domain.translation

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
     * Whether this device can translate descriptions for the current user right now: Gemini Nano is
     * ready **and** the device's language is not already English. Both call sites gate on this single
     * method instead of re-deriving the same two conditions.
     */
    suspend fun isSupported(): Boolean

    /**
     * Translates [description] for game [gameId] into the device's language, or returns `null` when it
     * cannot — blank or too long input, an unsupported device, or a model failure. `null` is not an
     * error to surface: the caller's whole contract is "show the original text instead", so there is no
     * typed failure here the way `RepositoryError` models network failures.
     */
    suspend fun translate(gameId: Int, description: String): String?
}
