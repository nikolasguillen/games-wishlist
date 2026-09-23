package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class SortingUiModel(
    val sortType: SearchSort,
    val label: UiText,
    val selected: Boolean,
    val descending: Boolean
)