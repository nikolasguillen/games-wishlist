package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

data class GameDetailUiModel(
    val id: Int,
    val name: UiText,
    val description: UiText,
    val images: List<String>,
    val gameType: UiText,
    val rating: RatingUiModel?,
    val releaseInfo: ReleaseInfoUiModel?,
    val platforms: List<UiText>,
    val genres: List<UiText>,
    val companyInfo: UiText,
    val isWishlisted: Boolean,
    val personalDetails: GameDetailPersonalUiModel,
    val relatedGames: List<RelatedGamesUiModel>
)
