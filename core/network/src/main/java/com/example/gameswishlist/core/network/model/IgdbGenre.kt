package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * Represents a game genre.
 *
 * @property id Internal IGDB unique identifier for the genre.
 * @property name The name of the genre (e.g., "Adventure", "Strategy").
 */
@JsonClass(generateAdapter = true)
data class IgdbGenre(
    val id: Int,
    val name: String
)
