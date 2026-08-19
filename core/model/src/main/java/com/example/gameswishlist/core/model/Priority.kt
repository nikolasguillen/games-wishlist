package com.example.gameswishlist.core.model

/**
 * How badly the user wants a game, set by hand on a wishlisted entry.
 *
 * As with [GameStatus], the entries carry an explicit [id] because that is what is persisted and what
 * sorting runs on — never the enum name and never the label, which lives in the UI layer.
 *
 * @property id Stable identifier written to the database, ordered from least to most urgent. Do not
 *   renumber existing entries.
 */
enum class Priority(val id: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        /** Falls back to [LOW] for an unknown [id] rather than throwing. */
        fun fromId(id: Int): Priority = entries.find { it.id == id } ?: LOW
    }
}
