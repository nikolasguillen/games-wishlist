package com.example.gameswishlist.core.model

data class Game(
    val id: Int,
    val name: String,
    val description: String = "",
    val released: String? = null,
    val backgroundImage: String? = null,
    val rating: Double = 0.0,
    val metaCritic: Int? = null,
    val platforms: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val isWishlisted: Boolean = false,
    val notes: String = "",
    val priority: Priority = Priority.LOW,
    val status: GameStatus = GameStatus.WANT_TO_BUY
)