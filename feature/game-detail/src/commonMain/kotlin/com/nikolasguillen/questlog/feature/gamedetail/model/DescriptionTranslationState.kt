package com.nikolasguillen.questlog.feature.gamedetail.model

import androidx.compose.runtime.Immutable

/**
 * The on-device translation lifecycle for the currently shown description, independent of
 * [GameDetailContentState]: the game can be [GameDetailContentState.Success] while this is still
 * [InProgress].
 */
@Immutable
internal sealed interface DescriptionTranslationState {
    /** The device cannot translate at all — the card offers no action. */
    data object Unavailable : DescriptionTranslationState
    /** Translation is possible and has not been asked for: original text plus a translate action. */
    data object Available : DescriptionTranslationState
    data object InProgress : DescriptionTranslationState
    data class Ready(val text: String) : DescriptionTranslationState
    /** The attempt failed: original text plus a retry action. */
    data object Failed : DescriptionTranslationState
}
