package com.example.gameswishlist.core.model

data class WishlistList(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val gameCount: Int = 0
)