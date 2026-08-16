package com.example.gameswishlist.feature.search.model

import com.example.gameswishlist.core.ui.model.UiText

internal data class SortingUiModel(
    val sortType: SearchSort,
    val label: UiText,
    val selected: Boolean,
    val descending: Boolean
)