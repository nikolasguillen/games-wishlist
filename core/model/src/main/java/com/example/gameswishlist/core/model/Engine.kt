package com.example.gameswishlist.core.model

/**
 * A game engine a title was built with, as catalogued by IGDB.
 *
 * @property id Internal IGDB unique identifier.
 * @property name The engine's name (e.g., Unreal Engine, Unity).
 */
data class Engine(
    val id: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
