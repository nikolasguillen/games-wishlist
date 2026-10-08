package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerContentState
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.owned_platforms_clear_search_action
import com.nikolasguillen.questlog.core.ui.resources.owned_platforms_empty
import com.nikolasguillen.questlog.core.ui.resources.owned_platforms_no_results
import com.nikolasguillen.questlog.core.ui.resources.retry
import org.jetbrains.compose.resources.stringResource

/**
 * The body of the owned-platforms picker: one of its four states, with the list rendered as selectable
 * rows. It holds no state of its own — every tap goes out through [onToggle] — which is what lets Settings
 * and the welcome flow each drive it from their own ViewModel.
 *
 * @param header Optional content emitted as the first item of the list in [PlatformPickerContentState.Success],
 * so it scrolls away with the rows. In the other states there is nothing long to scroll, so the caller
 * draws it itself.
 */
@Composable
fun PlatformPickerList(
    state: PlatformPickerContentState,
    onToggle: (platformId: Int) -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    when (state) {
        is PlatformPickerContentState.Loading -> LoadingPage(modifier = modifier)

        is PlatformPickerContentState.Empty -> EmptyPage(
            message = stringResource(Res.string.owned_platforms_empty),
            icon = Icons.Default.SportsEsports,
            actionLabel = UiText.StringResource(Res.string.retry),
            onActionClick = onRetry,
            modifier = modifier
        )

        is PlatformPickerContentState.NoSearchResults -> EmptyPage(
            message = stringResource(Res.string.owned_platforms_no_results),
            icon = Icons.Default.SearchOff,
            actionLabel = UiText.StringResource(Res.string.owned_platforms_clear_search_action),
            onActionClick = onClearQuery,
            modifier = modifier
        )

        is PlatformPickerContentState.Success -> LazyColumn(
            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.extraLarge),
            modifier = modifier.fillMaxWidth()
        ) {
            if (header != null) {
                item(key = HEADER_KEY) { header() }
            }
            items(state.platforms, key = { it.id }) { platform ->
                PlatformRow(platform = platform, onToggle = { onToggle(platform.id) })
            }
        }
    }
}

private const val HEADER_KEY = "platform_picker_header"

private val previewPlatforms = listOf(
    PlatformPickerItemUiModel(id = 167, name = "PlayStation 5", abbreviation = "PS5", isSelected = true),
    PlatformPickerItemUiModel(id = 6, name = "PC (Microsoft Windows)", abbreviation = "PC", isSelected = false),
    PlatformPickerItemUiModel(id = 471, name = "Meta Quest 3", abbreviation = null, isSelected = false)
)

@QuestLogPreviews
@Composable
private fun PlatformPickerListSuccessPreview() {
    QuestLogTheme {
        PlatformPickerList(
            state = PlatformPickerContentState.Success(previewPlatforms),
            onToggle = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun PlatformPickerListEmptyPreview() {
    QuestLogTheme {
        PlatformPickerList(
            state = PlatformPickerContentState.Empty,
            onToggle = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun PlatformPickerListNoResultsPreview() {
    QuestLogTheme {
        PlatformPickerList(
            state = PlatformPickerContentState.NoSearchResults,
            onToggle = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun PlatformPickerListLoadingPreview() {
    QuestLogTheme {
        PlatformPickerList(
            state = PlatformPickerContentState.Loading,
            onToggle = {},
            onClearQuery = {},
            onRetry = {}
        )
    }
}
