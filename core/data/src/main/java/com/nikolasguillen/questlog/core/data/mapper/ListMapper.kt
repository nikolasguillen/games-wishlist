package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.relation.ListWithGameCount
import com.nikolasguillen.questlog.core.model.WishlistList

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
