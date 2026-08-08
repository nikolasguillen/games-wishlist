package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

@Immutable
internal data class RelatedGamesUiModel(
    val title: UiText,
    val games: List<GameItemUiModel>
)