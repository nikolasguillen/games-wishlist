package com.example.gameswishlist.core.model

data class Platform(
    val id: Int,
    val name: String,
    val abbreviation: String? = null,
    val generation: Int? = null,
    val category: Int? = null,
    val platformFamily: Int? = null
) {
    override fun toString(): String {
        return name
    }
}
