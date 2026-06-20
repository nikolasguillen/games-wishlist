package com.example.gameswishlist.core.ui.model

data class GameItemUiModel(
    val id: Int,
    val name: String,
    val coverImage: String?,
    val ratingText: UiText,
    val releaseDateText: UiText,
    val developer: UiText?,
    val platforms: UiText?
) {
    companion object {
        fun getDummy() = GameItemUiModel(
            id = 1,
            name = "The Witcher 3: Wild Hunt",
            coverImage = "https://media.rawg.io/media/games/618/618c49a64e2f469d6107ba9357d812d6.jpg",
            ratingText = UiText.DynamicString("9.5"),
            releaseDateText = UiText.DynamicString("2015-05-19"),
            developer = UiText.DynamicString("CD Projekt Red"),
            platforms = UiText.DynamicString("PC (Microsoft Windows), PlayStation 4, Xbox One")
        )
    }
}
