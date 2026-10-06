package com.nikolasguillen.questlog.core.model

/**
 * How a wishlist's games are laid out on its detail screen.
 *
 * The entries carry an explicit [id] because that is what is persisted — never the enum name.
 *
 * @property id Stable identifier written to DataStore. Never renumber existing entries, only append.
 */
enum class WishlistViewMode(val id: Int) {
    LIST(0),
    GRID(1);

    companion object {
        /** Falls back to [LIST] for an unknown [id] rather than throwing. */
        fun fromId(id: Int): WishlistViewMode = entries.find { it.id == id } ?: LIST
    }
}
