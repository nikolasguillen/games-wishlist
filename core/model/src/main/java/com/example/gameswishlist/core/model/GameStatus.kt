package com.example.gameswishlist.core.model

/**
 * Where a game sits in the user's collection.
 *
 * The entries carry an explicit [id] because that is what is persisted and what filtering and sorting run
 * on — never the enum name and never the label, which lives in the UI layer.
 *
 * @property id Stable identifier written to the database. Do not renumber existing entries.
 */
enum class GameStatus(val id: Int) {
    WANT_TO_BUY(1),
    BOUGHT(2),
    PLAYING(3),
    COMPLETED(4),
    DROPPED(5);

    companion object {
        /** Falls back to [WANT_TO_BUY] for an unknown [id] rather than throwing. */
        fun fromId(id: Int): GameStatus = entries.find { it.id == id } ?: WANT_TO_BUY
    }
}
