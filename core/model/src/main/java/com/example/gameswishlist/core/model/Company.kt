package com.example.gameswishlist.core.model

/**
 * A company involved in a game. Whether it developed it, published it, or both, is a property of the
 * relation and lives in `GameCompanyCrossRef`, not here.
 *
 * @property id Internal IGDB unique identifier.
 * @property name The company's name.
 */
data class Company(
    val id: Int,
    val name: String
) {
    override fun toString(): String {
        return name
    }
}
