package com.nikolasguillen.questlog.core.database.relation

import androidx.room.Embedded
import com.nikolasguillen.questlog.core.database.entity.ListEntity

data class ListWithGameCount(
    @Embedded val list: ListEntity,
    val gameCount: Int
)
