package com.nikolasguillen.questlog.core.model

/**
 * Where a game sits in the user's collection.
 *
 * The entries carry an explicit [id] because that is what is persisted and what filtering and sorting run
 * on — never the enum name and never the label, which lives in the UI layer.
 *
 * @property id Stable identifier written to the database. Do not renumber existing entries.
 * @property requiresRelease Whether the status only makes sense once the game is out. A pre-order is a
 * purchase, so [WANT_TO_BUY] and [BOUGHT] stay available before launch; playing, finishing or dropping a
 * game does not.
 */
enum class GameStatus(val id: Int, val requiresRelease: Boolean) {
    WANT_TO_BUY(1, requiresRelease = false),
    BOUGHT(2, requiresRelease = false),
    PLAYING(3, requiresRelease = true),
    COMPLETED(4, requiresRelease = true),
    DROPPED(5, requiresRelease = true);

    companion object {
        /** Falls back to [WANT_TO_BUY] for an unknown [id] rather than throwing. */
        fun fromId(id: Int): GameStatus = entries.find { it.id == id } ?: WANT_TO_BUY
    }
}
