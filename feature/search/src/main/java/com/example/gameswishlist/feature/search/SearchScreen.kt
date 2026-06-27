@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gameswishlist.feature.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.ui.component.CustomFab
import com.example.gameswishlist.core.ui.component.StatusBarProtection
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.components.SearchFilterBottomSheet
import com.example.gameswishlist.feature.search.components.SearchMainContent
import com.example.gameswishlist.feature.search.components.SearchSortBottomSheet
import com.example.gameswishlist.feature.search.components.SearchTopBar
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchHistoryUiModel
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SearchScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onGameClick = onGameClick,
        modifier = modifier
    )
}

@Composable
fun SearchScreenContent(
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {

    // 1. UI States & Behaviors
    val searchBarState = rememberContainedSearchBarState()
    val textFieldState = rememberTextFieldState()
    val gridState = rememberLazyGridState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val scope = rememberCoroutineScope()


    // 2. Derived States (Scroll logic)
    val isScrolled by remember(scrollBehavior) {
        derivedStateOf {
            if (scrollBehavior.scrollOffsetLimit != 0f) {
                val fraction =
                    1 - ((scrollBehavior.scrollOffsetLimit - scrollBehavior.contentOffset)
                        .coerceIn(
                            scrollBehavior.scrollOffsetLimit,
                            0f
                        ) / scrollBehavior.scrollOffsetLimit)
                fraction > 0.01f
            } else false
        }
    }

    val showScrollToTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 1 }
    }

    // 3. UI Actions
    val onScrollToTop = remember(scrollBehavior, gridState) {
        suspend {
            scrollBehavior.contentOffset = 0f
            scrollBehavior.scrollOffset = 0f
            gridState.animateScrollToItem(0)
        }
    }

    val onResetScroll = remember(scrollBehavior, gridState) {
        suspend {
            scrollBehavior.contentOffset = 0f
            scrollBehavior.scrollOffset = 0f
            gridState.scrollToItem(0)
        }
    }

    val onSearch: (String) -> Unit =
        remember(onResetScroll, textFieldState, searchBarState, onEvent, scope) {
            { query ->
                scope.launch { onResetScroll() }
                textFieldState.setTextAndPlaceCursorAtEnd(query)
                scope.launch { searchBarState.animateToCollapsed() }
                onEvent(SearchUiEvent.OnSearchTriggered(query))
            }
        }

    val onGameClickWithCollapse: (Int) -> Unit =
        remember(onGameClick, searchBarState, scope) {
            { gameId ->
                scope.launch {
                    onGameClick(gameId)
                    delay(300.milliseconds)
                    searchBarState.snapTo(0F)
                }
            }
        }

    // 4. Dynamic Styles
    val backgroundColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.appColors.searchBarScrolledContainerColor
        else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "topBarBackground"
    )

    // 5. Layout
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.appColors.appBackground,
        topBar = {
            SearchTopBar(
                uiState = uiState,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                scrollBehavior = scrollBehavior,
                onSearch = onSearch,
                onGameClick = onGameClickWithCollapse,
                onEvent = onEvent,
                backgroundColor = backgroundColor
            )
        },
        floatingActionButton = {
            CustomFab(
                onClick = { scope.launch { onScrollToTop() } },
                modifier = Modifier.animateFloatingActionButton(
                    visible = showScrollToTop,
                    alignment = Alignment.Center
                )
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.scroll_to_top_content_description)
                )
            }
        }
    ) { innerPadding ->
        SearchMainContent(
            contentState = uiState.contentState,
            onEvent = onEvent,
            onGameClick = onGameClickWithCollapse,
            gridState = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )

        SearchFilterBottomSheet(state = uiState.filtersBottomSheetState, onEvent = onEvent)
        SearchSortBottomSheet(state = uiState.sortBottomSheetState, onEvent = onEvent)

        StatusBarProtection(color = backgroundColor)
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                contentState = SearchContentState.Success(
                    games = listOf(
                        GameItemUiModel.getDummy(),
                        GameItemUiModel.getDummy().copy(id = 2, name = "The Witcher 2")
                    ),
                    filters = listOf(
                        GameFilterUiModel.Platform(
                            id = 0,
                            label = UiText.DynamicString("PC"),
                            selected = true
                        ),
                        GameFilterUiModel.Platform(
                            id = 1,
                            label = UiText.DynamicString("PlayStation 4"),
                            selected = false

                        ),
                        GameFilterUiModel.Platform(
                            id = 2,
                            label = UiText.DynamicString("Xbox One"),
                            selected = false
                        )
                    )
                )
            ),
            onEvent = {},
            onGameClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenInitialWithHistoryPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                history = SearchHistoryUiModel(
                    queries = listOf("The Witcher", "Cyberpunk 2077"),
                    games = listOf(
                        GameItemUiModel.getDummy(),
                        GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
                    )
                )
            ),
            onEvent = {},
            onGameClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenLoadingPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                contentState = SearchContentState.Loading
            ),
            onEvent = {},
            onGameClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenInitialPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(),
            onEvent = {},
            onGameClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchScreenEmptyPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                contentState = SearchContentState.Empty
            ),
            onEvent = {},
            onGameClick = {}
        )
    }
}
