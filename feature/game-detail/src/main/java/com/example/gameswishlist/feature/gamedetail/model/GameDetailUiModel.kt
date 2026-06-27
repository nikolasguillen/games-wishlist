package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

data class GameDetailUiModel(
    val id: Int,
    val name: UiText,
    val description: UiText,
    val images: List<String>,
    val gameType: UiText,
    val ratingText: UiText,
    val platforms: UiText,
    val releaseDates: List<Pair<UiText, UiText>>,
    val genres: List<UiText>,
    val developers: UiText,
    val publishers: UiText,
    val engines: UiText,
    val isWishlisted: Boolean,
    val personalDetails: GameDetailPersonalUiModel,
    val relatedGames: List<RelatedGamesUiModel>
)
