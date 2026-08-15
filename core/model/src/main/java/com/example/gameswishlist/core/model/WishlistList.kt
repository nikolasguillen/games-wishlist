package com.example.gameswishlist.core.model

/**
 * A user-created list of games. The default wishlist is one of these, with the id fixed by
 * [WishlistConstants.DEFAULT_WISHLIST_ID].
 *
 * @property id Room-generated identifier; 0 means the list has not been persisted yet.
 * @property name The name the user gave the list. Raw text, not a resource — the default list's name is
 *   seeded from `strings.xml` when the database is created.
 * @property description Optional free text describing the list.
 * @property icon The icon shown next to the list, or null to fall back to the default.
 * @property coverImagePath Absolute path to the cover image copied into app storage, or null if the list
 *   has none. Not a remote URL.
 * @property gameCount How many games the list holds. Derived at query time, never stored.
 */
data class WishlistList(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: WishlistIcon? = null,
    val coverImagePath: String? = null,
    val gameCount: Int = 0
)
