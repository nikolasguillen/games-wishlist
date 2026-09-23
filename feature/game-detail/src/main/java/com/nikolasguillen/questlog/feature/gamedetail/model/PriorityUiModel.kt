package com.nikolasguillen.questlog.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class PriorityUiModel(
    val id: Int,
    val label: UiText,
    val selected: Boolean
)