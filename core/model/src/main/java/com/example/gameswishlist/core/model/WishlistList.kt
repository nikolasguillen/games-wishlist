package com.example.gameswishlist.core.model

data class WishlistList(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val gameCount: Int = 0
)

// TODO creare UI model con spunta se lista contiene già il gioco ed eventualmente una leading icon a scelta dell'utente