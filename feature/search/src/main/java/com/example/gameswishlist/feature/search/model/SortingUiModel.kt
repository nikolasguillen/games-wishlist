package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.UiText

@Immutable
internal data class SortingUiModel(
    val sortType: SearchSort,
    val label: UiText,
    val selected: Boolean,
    val descending: Boolean
)