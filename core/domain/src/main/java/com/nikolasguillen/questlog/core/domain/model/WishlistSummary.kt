package com.nikolasguillen.questlog.core.domain.model

import com.nikolasguillen.questlog.core.model.WishlistList

/**
 * Domain model representing a wishlist as the overview list shows it: the list itself and whether it is
 * the wishlist currently set as the default.
 *
 * @property list The wishlist list information.
 * @property isDefault `true` when [list] is the wishlist currently set as the default.
 */
data class WishlistSummary(
    val list: WishlistList,
    val isDefault: Boolean
)
