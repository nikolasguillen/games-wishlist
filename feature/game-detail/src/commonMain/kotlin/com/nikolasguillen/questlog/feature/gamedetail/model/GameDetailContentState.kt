package com.nikolasguillen.questlog.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal sealed interface GameDetailContentState {
    data object Loading : GameDetailContentState
    data class Success(val game: GameDetailUiModel) : GameDetailContentState
    data class Error(val message: UiText) : GameDetailContentState
}
