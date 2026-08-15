package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * UI representation of a game suggestion.
 */
@Immutable
data class GameSuggestionUiModel(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val subtitle: String
)
