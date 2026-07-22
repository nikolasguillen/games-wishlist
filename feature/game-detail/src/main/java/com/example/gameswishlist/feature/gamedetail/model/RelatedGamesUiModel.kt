package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

internal data class RelatedGamesUiModel(
    val title: UiText,
    val games: List<GameItemUiModel>
)