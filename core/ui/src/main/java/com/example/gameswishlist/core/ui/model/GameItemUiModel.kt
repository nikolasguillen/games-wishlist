package com.example.gameswishlist.core.ui.model

data class GameItemUiModel(
    val id: Int,
    val name: String,
    val coverImage: String?,
    val rating: Int,
    val releaseDateText: UiText,
    val releaseYear: String?,
    val developer: String?,
    val platforms: UiText?
) {
    companion object {
        fun getDummy() = GameItemUiModel(
            id = 1,
            name = "The Witcher 3: Wild Hunt",
            coverImage = "https://media.rawg.io/media/games/618/618c49a64e2f469d6107ba9357d812d6.jpg",
            rating = 95,
            releaseDateText = UiText.DynamicString("2015-05-19"),
            releaseYear = "2015",
            developer = "CD Projekt Red",
            platforms = UiText.DynamicString("PC (Microsoft Windows), PlayStation 4, Xbox One")
        )
    }
}
