package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.runtime.Immutable

/**
 * The on-device translation lifecycle for the currently shown description, independent of
 * [GameDetailContentState]: the game can be [GameDetailContentState.Success] while this is still
 * [InProgress].
 */
@Immutable
internal sealed interface DescriptionTranslationState {
    /** Translation is disabled, unsupported on this device, or failed — show the original text. */
    data object Off : DescriptionTranslationState
    data object InProgress : DescriptionTranslationState
    data class Ready(val text: String) : DescriptionTranslationState
}
