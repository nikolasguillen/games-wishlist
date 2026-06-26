package com.example.gameswishlist.feature.gamedetail.model

data class GameDetailUiModel(
    val id: Int,
    val name: String,
    val description: String,
    val backgroundImage: String?,
    val rating: Double,
    val metaCritic: Int?,
    val platforms: List<String>,
    val genres: List<String>,
    val personalDetails: GameDetailPersonalUiModel
)
