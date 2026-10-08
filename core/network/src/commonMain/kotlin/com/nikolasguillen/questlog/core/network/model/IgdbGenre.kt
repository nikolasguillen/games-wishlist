package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.Serializable

/**
 * Represents a game genre.
 *
 * @property id Internal IGDB unique identifier for the genre.
 * @property name The name of the genre (e.g., "Adventure", "Strategy").
 */
@Serializable
data class IgdbGenre(
    val id: Int,
    val name: String
)
