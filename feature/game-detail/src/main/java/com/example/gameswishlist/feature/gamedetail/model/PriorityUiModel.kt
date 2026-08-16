package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.UiText

@Immutable
internal data class PriorityUiModel(
    val id: Int,
    val label: UiText,
    val selected: Boolean
)