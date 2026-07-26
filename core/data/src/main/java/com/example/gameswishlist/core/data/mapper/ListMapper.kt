package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.ListWithGameCount
import com.example.gameswishlist.core.model.WishlistList

fun ListWithGameCount.toWishlistList(): WishlistList {
    return WishlistList(
        id = list.id,
        name = list.name,
        description = list.description,
        icon = list.icon,
        coverImagePath = list.coverImagePath,
        gameCount = gameCount
    )
}
