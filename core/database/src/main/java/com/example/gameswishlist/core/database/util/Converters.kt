package com.example.gameswishlist.core.database.util

import androidx.room.TypeConverter
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.WishlistIcon

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

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
}
