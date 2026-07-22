package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

internal data class GameDetailPersonalUiModel(
    val notes: UiText,
    val availableStatuses: List<GameStatusUiModel>,
    val availablePriorities: List<PriorityUiModel>
)
