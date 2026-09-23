package com.nikolasguillen.questlog.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class RelatedGamesUiModel(
    val title: UiText,
    val games: List<GameItemUiModel>
)