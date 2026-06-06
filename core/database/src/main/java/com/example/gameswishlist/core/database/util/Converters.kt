package com.example.gameswishlist.core.database.util

import androidx.room.TypeConverter
import com.example.gameswishlist.core.model.GameStatus

class Converters {
    @TypeConverter
    fun fromGameStatus(status: GameStatus): String {
        return status.name
    }

    @TypeConverter
    fun toGameStatus(status: String): GameStatus {
        return GameStatus.valueOf(status)
    }
}
