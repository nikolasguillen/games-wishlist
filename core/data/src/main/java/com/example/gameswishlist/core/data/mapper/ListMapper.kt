package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.ListEntity
import com.example.gameswishlist.core.database.relation.ListWithGameCount
import com.example.gameswishlist.core.model.WishlistList

fun ListEntity.toWishlistList(gameCount: Int = 0): WishlistList {
    return WishlistList(
        id = id,
        name = name,
        description = description,
        icon = icon,
        coverImagePath = coverImagePath,
        gameCount = gameCount
    )
}

fun ListWithGameCount.toWishlistList(): WishlistList = list.toWishlistList(gameCount = gameCount)
