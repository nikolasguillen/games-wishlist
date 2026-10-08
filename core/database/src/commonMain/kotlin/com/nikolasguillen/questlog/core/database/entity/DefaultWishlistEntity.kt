package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Points at the wishlist the user picked as their default.
 *
 * The primary key is fixed at `0`, so the table can hold at most one row: that is what makes "exactly one
 * default" true. `RESTRICT` makes the database itself refuse to delete the list this row points at,
 * whichever code path issues the delete.
 */
@Entity(
    tableName = "default_wishlist",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("listId")]
)
data class DefaultWishlistEntity(
    @PrimaryKey val id: Long = 0,
    val listId: Long
)
