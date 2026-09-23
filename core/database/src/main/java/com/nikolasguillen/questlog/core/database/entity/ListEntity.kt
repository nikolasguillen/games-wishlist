package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nikolasguillen.questlog.core.model.WishlistIcon

@Entity(tableName = "wishlists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val icon: WishlistIcon? = null,
    val coverImagePath: String? = null
)
