package com.example.gameswishlist.feature.settings.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface TranslationModelRowState {
    /** Unsupported device, or already English — the row is absent entirely. */
    data object Hidden : TranslationModelRowState
    data object Downloadable : TranslationModelRowState
    data class Downloading(val fraction: Float?) : TranslationModelRowState
    data object Ready : TranslationModelRowState
    data object Failed : TranslationModelRowState
}
