package com.nikolasguillen.questlog.core.database.util

import androidx.room.TypeConverter
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.WishlistIcon

class Converters {
    @TypeConverter
    fun fromGameStatus(status: GameStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toGameStatus(status: String?): GameStatus? {
        return status?.let { GameStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromWishlistIcon(icon: WishlistIcon?): String? {
        return icon?.name
    }

    @TypeConverter
    fun toWishlistIcon(icon: String?): WishlistIcon? {
        return icon?.let { WishlistIcon.valueOf(it) }
    }
}
