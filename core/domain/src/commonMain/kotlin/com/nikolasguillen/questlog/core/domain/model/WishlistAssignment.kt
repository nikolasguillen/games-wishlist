package com.nikolasguillen.questlog.core.domain.model

import com.nikolasguillen.questlog.core.model.WishlistList

/**
 * Domain model representing a wishlist list, whether a specific game is assigned to it, and whether it is
 * the wishlist currently set as the default.
 *
 * @property list The wishlist list information.
 * @property isAssigned True if the game is present in this list.
 * @property isDefault `true` when [list] is the wishlist currently set as the default.
 */
data class WishlistAssignment(
    val list: WishlistList,
    val isAssigned: Boolean,
    val isDefault: Boolean
)
