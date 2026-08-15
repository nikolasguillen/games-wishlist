package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * Represents a game engine.
 *
 * @property id Internal IGDB unique identifier.
 * @property name The name of the engine (e.g., "Unreal Engine 5").
 */
@JsonClass(generateAdapter = true)
data class IgdbGameEngine(
    val id: Int,
    val name: String
)
