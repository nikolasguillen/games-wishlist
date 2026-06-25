package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority

data class GameDetailUiModel(
    val id: Int,
    val name: String,
    val description: String,
    val backgroundImage: String?,
    val rating: Double,
    val metaCritic: Int?,
    val platforms: List<String>,
    val genres: List<String>,
    val status: GameStatus,
    val priority: Priority,
    val notes: String
)

fun Game.toUiModel(): GameDetailUiModel {
    return GameDetailUiModel(
        id = id,
        name = name,
        description = description,
        backgroundImage = backgroundImage,
        rating = rating,
        metaCritic = metaCritic,
        platforms = platforms.map { it.name },
        genres = genres.map { it.name },
        status = status,
        priority = priority,
        notes = notes
    )
}
