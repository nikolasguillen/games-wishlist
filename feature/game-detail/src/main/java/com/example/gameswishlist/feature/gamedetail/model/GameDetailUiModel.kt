package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

data class GameDetailUiModel(
    val id: Int,
    val name: String,
    val description: String,
    val images: List<String>,
    val gameType: UiText,
    val ratingText: UiText,
    val platforms: List<String>,
    val genres: List<String>,
    val isWishlisted: Boolean,
    val personalDetails: GameDetailPersonalUiModel,
    val relatedGames: List<RelatedGamesUiModel>
)