package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal sealed interface GameFilterUiModel {
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

    data class GameType(
        override val id: Int,
        override val label: UiText,
        override val selected: Boolean
    ) : GameFilterUiModel
}