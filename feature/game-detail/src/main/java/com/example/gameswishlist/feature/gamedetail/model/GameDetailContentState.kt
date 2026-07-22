package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

internal sealed interface GameDetailContentState {
    data object Loading : GameDetailContentState
    data class Success(val game: GameDetailUiModel) : GameDetailContentState
    data class Error(val message: UiText) : GameDetailContentState
}
