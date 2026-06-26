package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState

/**
 * The high-level content switcher for the Game Detail screen.
 * Handles Loading, Error, and Success states.
 */
@Composable
fun GameDetailMainContent(
    uiState: GameDetailUiState,
    onEvent: (GameDetailUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (val content = uiState.contentState) {
        is GameDetailContentState.Loading -> {
            DetailErrorLoadingWrapper(onBackClick = onBackClick) {
                LoadingPage()
            }
        }

        is GameDetailContentState.Error -> {
            DetailErrorLoadingWrapper(onBackClick = onBackClick) {
                ErrorPage(message = content.message)
            }
        }

        is GameDetailContentState.Success -> {
            GameDetailSuccessContent(
                game = content.game,
                onBackClick = onBackClick,
                onEvent = onEvent,
                modifier = modifier
            )
        }
    }
}
