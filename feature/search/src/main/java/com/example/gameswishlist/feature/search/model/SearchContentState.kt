package com.example.gameswishlist.feature.search.model

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

sealed interface SearchContentState {
    data object Initial : SearchContentState
    data object Loading : SearchContentState
    data object Empty : SearchContentState
    data class Success(
        val games: List<GameItemUiModel>,
        val filters: List<GameFilterUiModel>,
        val allGames: List<Game> = emptyList()
    ) : SearchContentState
    data class Error(val message: UiText) : SearchContentState
}
