package com.example.gameswishlist.core.model

enum class GameStatus(val id: Int) {
    WANT_TO_BUY(1),
    BOUGHT(2),
    PLAYING(3),
    COMPLETED(4),
    DROPPED(5);

    companion object {
        fun fromId(id: Int): GameStatus = entries.find { it.id == id } ?: WANT_TO_BUY
    }
}