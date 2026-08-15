package com.example.gameswishlist.core.model

/**
 * A platform a game runs on, as catalogued by IGDB.
 *
 * Everything past the name is optional because IGDB fills these in unevenly, especially for older or
 * niche hardware.
 *
 * @property id Internal IGDB unique identifier. Filtering runs on this, never on [name].
 * @property name The platform's full name (e.g., PlayStation 5).
 * @property abbreviation The short form IGDB publishes (e.g., PS5), when it has one.
 * @property generation Hardware generation number.
 * @property category IGDB's platform category id (console, handheld, computer, …).
 * @property platformFamily IGDB's family id, grouping a manufacturer's platforms together.
 */
data class Platform(
    val id: Int,
    val name: String,
    val abbreviation: String? = null,
    val generation: Int? = null,
    val category: Int? = null,
    val platformFamily: Int? = null
) {
    override fun toString(): String {
        return name
    }
}
