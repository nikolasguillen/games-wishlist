package com.example.gameswishlist.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.EmptyPage
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.feature.settings.components.PlatformRow
import com.example.gameswishlist.feature.settings.components.PlatformSearchField
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsContentState
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsUiEvent
import com.example.gameswishlist.feature.settings.model.OwnedPlatformsUiState
import com.example.gameswishlist.feature.settings.model.PlatformUiModel
import com.example.gameswishlist.core.ui.R as CoreUiR

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun OwnedPlatformsScreen(
    viewModel: OwnedPlatformsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OwnedPlatformsContent(
        state = state,
        searchFieldState = viewModel.textFieldState,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

/**
 * The "My platforms" picker. Every tap is stored immediately, so there is no confirm button and no
 * unsaved state to lose on back.
 *
 * The search field and the caption stay put while only the list scrolls, so the reason the list is
 * short never scrolls away from the results it explains.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OwnedPlatformsContent(
    state: OwnedPlatformsUiState,
    searchFieldState: TextFieldState,
    onEvent: (OwnedPlatformsUiEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.owned_platforms_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.back_content_description)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.mediumLarge),
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical = MaterialTheme.spacing.medium
                )
            ) {
                // One slot, always filled, pinned to two lines: the wording swaps as the selection
                // changes, but nothing below it moves. An appearing/disappearing caption shifted the
                // search field and the whole list on every tap that emptied or filled the selection.
                Text(
                    text = if (state.selectedCount == 0) {
                        stringResource(R.string.owned_platforms_no_filter)
                    } else {
                        pluralStringResource(
                            R.plurals.owned_platforms_filtering,
                            state.selectedCount,
                            state.selectedCount
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    minLines = 2
                )
                PlatformSearchField(
                    state = searchFieldState,
                    onClearQuery = { onEvent(OwnedPlatformsUiEvent.OnClearQuery) }
                )
            }

            when (val contentState = state.contentState) {
                is OwnedPlatformsContentState.Loading -> LoadingPage()

                is OwnedPlatformsContentState.Empty -> EmptyPage(
                    message = stringResource(R.string.owned_platforms_empty),
                    icon = Icons.Default.SportsEsports
                )

                is OwnedPlatformsContentState.NoSearchResults -> EmptyPage(
                    message = stringResource(R.string.owned_platforms_no_results),
                    icon = Icons.Default.SearchOff
                )

                is OwnedPlatformsContentState.Success -> LazyColumn(
                    contentPadding = PaddingValues(bottom = MaterialTheme.spacing.extraLarge),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(contentState.platforms, key = { it.id }) { platform ->
                        PlatformRow(
                            platform = platform,
                            onToggle = {
                                onEvent(OwnedPlatformsUiEvent.OnPlatformToggled(platform.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

private val previewPlatforms = listOf(
    PlatformUiModel(id = 167, name = "PlayStation 5", abbreviation = "PS5", isSelected = true),
    PlatformUiModel(id = 6, name = "PC (Microsoft Windows)", abbreviation = "PC", isSelected = true),
    PlatformUiModel(id = 130, name = "Nintendo Switch", abbreviation = "Switch", isSelected = false),
    PlatformUiModel(id = 169, name = "Xbox Series X|S", abbreviation = "Series X", isSelected = false),
    PlatformUiModel(id = 471, name = "Meta Quest 3", abbreviation = null, isSelected = false)
)

@Preview(showBackground = true)
@Composable
private fun OwnedPlatformsContentSuccessPreview() {
    GamesWishlistTheme {
        OwnedPlatformsContent(
            state = OwnedPlatformsUiState(
                contentState = OwnedPlatformsContentState.Success(previewPlatforms),
                selectedCount = 2
            ),
            searchFieldState = TextFieldState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OwnedPlatformsContentNoSelectionPreview() {
    GamesWishlistTheme {
        OwnedPlatformsContent(
            state = OwnedPlatformsUiState(
                contentState = OwnedPlatformsContentState.Success(
                    previewPlatforms.map { it.copy(isSelected = false) }
                ),
                selectedCount = 0
            ),
            searchFieldState = TextFieldState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OwnedPlatformsContentNoSearchResultsPreview() {
    GamesWishlistTheme {
        OwnedPlatformsContent(
            state = OwnedPlatformsUiState(
                contentState = OwnedPlatformsContentState.NoSearchResults,
                selectedCount = 2
            ),
            searchFieldState = TextFieldState("Dreamcast 2"),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OwnedPlatformsContentEmptyPreview() {
    GamesWishlistTheme {
        OwnedPlatformsContent(
            state = OwnedPlatformsUiState(contentState = OwnedPlatformsContentState.Empty),
            searchFieldState = TextFieldState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OwnedPlatformsContentLoadingPreview() {
    GamesWishlistTheme {
        OwnedPlatformsContent(
            state = OwnedPlatformsUiState(),
            searchFieldState = TextFieldState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}
