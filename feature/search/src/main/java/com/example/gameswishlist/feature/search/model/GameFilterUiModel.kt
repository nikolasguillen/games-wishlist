package com.example.gameswishlist.feature.search.model

import com.example.gameswishlist.core.ui.model.UiText

sealed interface GameFilterUiModel {
    val id: Int
    val label: UiText
    val selected: Boolean

    data class Platform(
        override val id: Int,
        override val label: UiText,
        override val selected: Boolean
    ) : GameFilterUiModel

    data class Genre(
        override val id: Int,
        override val label: UiText,
        override val selected: Boolean
    ) : GameFilterUiModel
}