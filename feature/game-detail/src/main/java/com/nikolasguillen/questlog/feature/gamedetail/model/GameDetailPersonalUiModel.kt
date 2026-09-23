package com.nikolasguillen.questlog.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class GameDetailPersonalUiModel(
    val notes: UiText,
    val availableStatuses: List<GameStatusUiModel>,
    val availablePriorities: List<PriorityUiModel>
)
