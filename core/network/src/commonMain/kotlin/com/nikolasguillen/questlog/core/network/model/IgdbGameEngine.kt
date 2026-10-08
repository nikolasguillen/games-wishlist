package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.Serializable

/**
 * Represents a game engine.
 *
 * @property id Internal IGDB unique identifier.
 * @property name The name of the engine (e.g., "Unreal Engine 5").
 */
@Serializable
data class IgdbGameEngine(
    val id: Int,
    val name: String
)
