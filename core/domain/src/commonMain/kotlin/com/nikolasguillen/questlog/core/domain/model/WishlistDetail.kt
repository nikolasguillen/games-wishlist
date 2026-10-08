package com.nikolasguillen.questlog.core.domain.model

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.WishlistList

/**
 * Domain model representing a wishlist list together with the games it contains.
 *
 * @property list The wishlist list information.
 * @property games The games currently assigned to this list.
 * @property isDefault `true` when [list] is the wishlist currently set as the default.
 */
data class WishlistDetail(
    val list: WishlistList,
    val games: List<Game>,
    val isDefault: Boolean
)
