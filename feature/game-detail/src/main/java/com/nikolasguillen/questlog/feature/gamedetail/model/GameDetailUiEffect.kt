package com.nikolasguillen.questlog.feature.gamedetail.model

import com.nikolasguillen.questlog.core.ui.model.UiText

internal sealed interface GameDetailUiEffect {
    data class ShareGame(val text: UiText) : GameDetailUiEffect
    data class NavigateToGame(val id: Int) : GameDetailUiEffect
}
