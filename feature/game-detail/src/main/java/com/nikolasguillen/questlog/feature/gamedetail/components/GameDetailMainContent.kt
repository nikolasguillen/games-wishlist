package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.ui.component.ErrorPage
import com.nikolasguillen.questlog.core.ui.component.LoadingPage
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailContentState
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiEvent
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiState

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
                ErrorPage(
                    message = content.message,
                    onRetryClick = { onEvent(GameDetailUiEvent.Retry) }
                )
            }
        }

        is GameDetailContentState.Success -> {
            GameDetailSuccessContent(
                game = content.game,
                descriptionTranslation = uiState.descriptionTranslation,
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
    QuestLogTheme {
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
    QuestLogTheme {
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
    QuestLogTheme {
        GameDetailMainContent(
            uiState = GameDetailUiState(
                contentState = GameDetailContentState.Success(GameDetailUiModel.getDummy())
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}
