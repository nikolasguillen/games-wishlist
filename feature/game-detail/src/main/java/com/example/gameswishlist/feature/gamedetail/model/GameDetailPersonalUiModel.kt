package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.UiText

@Immutable
internal data class GameDetailPersonalUiModel(
    val notes: UiText,
    val availableStatuses: List<GameStatusUiModel>,
    val availablePriorities: List<PriorityUiModel>
)
