package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

internal data class PriorityUiModel(
    val id: Int,
    val label: UiText,
    val selected: Boolean
)