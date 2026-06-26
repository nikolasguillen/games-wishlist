package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

sealed interface GameDetailUiEffect {
    data class ShareGame(val text: UiText) : GameDetailUiEffect
}
