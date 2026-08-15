package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState

/**
 * The high-level content switcher for the Game Detail screen.
 * Handles Loading, Error, and Success states.
 */
@Composable
internal fun GameDetailMainContent(
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

@Preview(showBackground = true)
@Composable
private fun GameDetailMainContentLoadingPreview() {
    GamesWishlistTheme {
        GameDetailMainContent(
            uiState = GameDetailUiState(contentState = GameDetailContentState.Loading),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailMainContentErrorPreview() {
    GamesWishlistTheme {
        GameDetailMainContent(
            uiState = GameDetailUiState(
                contentState = GameDetailContentState.Error(
                    UiText.DynamicString("No internet connection.")
                )
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailMainContentSuccessPreview() {
    GamesWishlistTheme {
        GameDetailMainContent(
            uiState = GameDetailUiState(
                contentState = GameDetailContentState.Success(GameDetailUiModel.getDummy())
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}
