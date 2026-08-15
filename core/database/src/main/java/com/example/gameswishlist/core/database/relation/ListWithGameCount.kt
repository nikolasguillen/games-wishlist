package com.example.gameswishlist.core.database.relation

import androidx.room.Embedded
import com.example.gameswishlist.core.database.entity.ListEntity

data class ListWithGameCount(
    @Embedded val list: ListEntity,
    val gameCount: Int
)
