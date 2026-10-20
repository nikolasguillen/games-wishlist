package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * Lifecycle of the grid of games on a character or developer page. It fails and recovers on its own: the header
 * and description stay when the games do not load.
 */
@Immutable
internal sealed interface EntityGamesContentState {
    data object Loading : EntityGamesContentState

    /** The catalogue links no game to this character or developer. */
    data object Empty : EntityGamesContentState

    data class Error(val message: UiText) : EntityGamesContentState

    data class Success(val games: List<GameItemUiModel>) : EntityGamesContentState
}
