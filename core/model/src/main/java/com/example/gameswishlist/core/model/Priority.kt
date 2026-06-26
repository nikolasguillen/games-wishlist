package com.example.gameswishlist.core.model

enum class Priority(val id: Int) {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        fun fromId(id: Int): Priority = entries.find { it.id == id } ?: LOW
    }
}