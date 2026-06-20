package com.example.gameswishlist.core.model

data class Game(
    val id: Int,
    val name: String,
    val description: String = "",
    val releaseDate: String? = null,
    val backgroundImage: String? = null,
    val rating: Double = 0.0,
    val metaCritic: Int? = null,
    val platforms: List<Platform> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val publishers: List<Company> = emptyList(),
    val developers: List<Company> = emptyList(),
    val isWishlisted: Boolean = false,
    val notes: String = "",
    val priority: Priority = Priority.LOW,
    val status: GameStatus = GameStatus.WANT_TO_BUY
)