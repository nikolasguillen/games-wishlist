package com.example.gameswishlist.core.model

/**
 * Core domain model representing a video game within the application.
 *
 * This model is used throughout the app as the single source of truth for game data,
 * transformed from network responses or database entities.
 *
 * @property id Unique identifier for the game.
 * @property name The display name of the title.
 * @property description A detailed overview or plot summary.
 * @property releaseDate Formatted release date (e.g., "YYYY-MM-DD").
 * @property backgroundImage URL for the game's primary artwork or screenshot.
 * @property rating The average rating or calculated relevance score (0.0 to 100.0).
 * @property ratingCount Total number of community ratings used for popularity calculations.
 * @property hypes Number of users anticipating or "hyping" this game (useful for new titles).
 * @property metaCritic The official Metacritic score, if available.
 * @property platforms List of platforms this game is released on.
 * @property genres Categories the game belongs to (e.g., RPG, Adventure).
 * @property publishers Companies responsible for publishing the title.
 * @property developers Studio(s) that developed the game.
 * @property isWishlisted Whether the game is currently in the user's local wishlist.
 * @property notes User-provided personal notes about the game.
 * @property priority User-defined priority for acquiring or playing the game.
 * @property status Current status of the game in the user's collection (e.g., Want to Buy).
 */
data class Game(
    val id: Int,
    val name: String,
    val description: String = "",
    val releaseDate: String? = null,
    val backgroundImage: String? = null,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val hypes: Int = 0,
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
