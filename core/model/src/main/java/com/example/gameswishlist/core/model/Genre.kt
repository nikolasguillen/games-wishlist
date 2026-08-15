package com.example.gameswishlist.core.model

/**
 * A game genre, as catalogued by IGDB.
 *
 * @property id Internal IGDB unique identifier. Filtering runs on this, never on [name].
 * @property name The genre's name (e.g., RPG, Shooter).
 */
data class Genre(
    val id: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
