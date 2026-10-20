package com.nikolasguillen.questlog.core.ui.model

import androidx.compose.runtime.Immutable

/**
 * The on-device translation lifecycle for the description currently shown (a game's, a character's or a
 * developer's), independent of whatever else on the screen is loading: the page can show its content while this
 * is still [InProgress].
 */
@Immutable
sealed interface DescriptionTranslationState {
    /** The device cannot translate at all — the card offers no action. */
    data object Unavailable : DescriptionTranslationState
    /** Translation is possible and has not been asked for: original text plus a translate action. */
    data object Available : DescriptionTranslationState
    data object InProgress : DescriptionTranslationState
    data class Ready(val text: String) : DescriptionTranslationState
    /** The attempt failed: original text plus a retry action. */
    data object Failed : DescriptionTranslationState
}
