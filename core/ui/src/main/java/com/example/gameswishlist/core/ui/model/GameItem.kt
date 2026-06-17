package com.example.gameswishlist.core.ui.model

data class GameItem(
    val id: Int,
    val name: String,
    val coverImage: String?,
    val ratingText: UiText,
    val releaseDateText: UiText
) {
    companion object {
        fun getDummy() = GameItem(
            id = 1,
            name = "The Witcher 3: Wild Hunt",
            coverImage = "https://media.rawg.io/media/games/618/618c49a64e2f469d6107ba9357d812d6.jpg",
            ratingText = UiText.DynamicString("9.5"),
            releaseDateText = UiText.DynamicString("2015-05-19")
        )
    }
}
