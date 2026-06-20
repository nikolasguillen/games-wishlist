package com.example.gameswishlist.core.model

data class Platform(
    val id: Int,
    val name: String,
    val abbreviation: String? = null
) {
    override fun toString(): String {
        return name
    }
}
