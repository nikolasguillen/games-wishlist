package com.nikolasguillen.questlog.core.domain.model

import com.nikolasguillen.questlog.core.model.WishlistList

/**
 * Domain model representing a wishlist list and whether a specific game is assigned to it.
 *
 * @property list The wishlist list information.
 * @property isAssigned True if the game is present in this list.
 */
data class WishlistAssignment(
    val list: WishlistList,
    val isAssigned: Boolean
)
