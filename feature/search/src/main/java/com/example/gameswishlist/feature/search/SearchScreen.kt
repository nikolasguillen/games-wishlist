@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gameswishlist.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.example.gameswishlist.feature.search.model.SearchUiEffect
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Lets the game-click navigation start before the search bar collapses behind it. */
private val SEARCH_BAR_COLLAPSE_DELAY = 300.milliseconds

// viewModel is the same instance for the route's whole lifetime, so ref-comparison skips correctly.
@Suppress("ParamsComparedByRef")
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onGameClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is SearchUiEffect.ShowSnackbar -> {
                        // showSnackbar suspends until its snackbar is dismissed, so it must not run on
                        // this collecting coroutine -- otherwise a fast second toggle sits buffered in
                        // the channel and its dismiss() never gets a chance to run until the first
                        // snackbar times out on its own. Launched on its own, each new effect can
                        // dismiss whatever is still showing (including a previous launch still waiting
                        // its turn) the instant it arrives.
                        launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(effect.message.asString(context))
                        }
                    }
                }
            }
        }
    }

    SearchScreenContent(
        uiState = uiState,
        textFieldState = viewModel.textFieldState,
        onEvent = viewModel::onEvent,
        onGameClick = onGameClick,
        onProfileClick = onProfileClick,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
internal fun SearchScreenContent(
    uiState: SearchUiState,
    textFieldState: TextFieldState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {

    // 1. UI States & Behaviors
    val searchBarState = rememberContainedSearchBarState()
    val gridState = rememberLazyGridState()
    val discoverListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // The feed and the results grid are two different list types with their own hoisted state, so
    // scroll-to-top and the FAB visibility must follow whichever one is currently on screen.
    val isDiscoverActive = uiState.contentState is SearchContentState.Idle

    // Recreated on switch so its offset always starts fresh for the list now on screen, instead
    // of carrying over whatever the previously visible list had scrolled to.
    val scrollBehavior = key(isDiscoverActive) {
        SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    }

    // 2. Derived States (Scroll logic)
    val isScrolled by remember(isDiscoverActive, discoverListState, gridState) {
        derivedStateOf {
            if (isDiscoverActive) {
                discoverListState.firstVisibleItemIndex > 0 || discoverListState.firstVisibleItemScrollOffset > 0
            } else {
                gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
            }
        }
    }

    val showScrollToTop by remember(isDiscoverActive) {
        derivedStateOf {
            if (isDiscoverActive) discoverListState.firstVisibleItemIndex > 1
            else gridState.firstVisibleItemIndex > 1
        }
    }

    // 3. UI Actions
    val onScrollToTop = remember(scrollBehavior, gridState, discoverListState, isDiscoverActive) {
        suspend {
            scrollBehavior.scrollState.contentOffset = 0f
            scrollBehavior.scrollState.scrollOffset = 0f
            if (isDiscoverActive) discoverListState.animateScrollToItem(0)
            else gridState.animateScrollToItem(0)
        }
    }

    val onResetScroll = remember(scrollBehavior, gridState) {
        suspend {
            scrollBehavior.scrollState.contentOffset = 0f
            scrollBehavior.scrollState.scrollOffset = 0f
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
                    delay(SEARCH_BAR_COLLAPSE_DELAY)
                    searchBarState.snapTo(0F)
                }
            }
        }

    // Only fires once a query is committed and the bar is collapsed: while expanded, the search
    // bar's own predictive-back handling owns the gesture (collapsing it instead).
    BackHandler(
        enabled = searchBarState.currentValue == SearchBarValue.Collapsed &&
            textFieldState.text.isNotEmpty()
    ) {
        onEvent(SearchUiEvent.OnClearSearch)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SearchTopBar(
                uiState = uiState,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                scrollBehavior = scrollBehavior,
                onSearch = onSearch,
                onGameClick = onGameClickWithCollapse,
                onEvent = onEvent,
                onProfileClick = onProfileClick,
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
            discoverState = uiState.discover,
            onEvent = onEvent,
            onGameClick = onGameClickWithCollapse,
            gridState = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            discoverListState = discoverListState
        )

        SearchFilterBottomSheet(state = uiState.filtersBottomSheetState, onEvent = onEvent)
        SearchSortBottomSheet(state = uiState.sortBottomSheetState, onEvent = onEvent)

        StatusBarProtection(color = backgroundColor)
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                contentState = SearchContentState.Success(
                    games = listOf(
                        GameItemUiModel.getDummy(),
                        GameItemUiModel.getDummy()
                            .copy(id = 2, name = "The Witcher 2")
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
            textFieldState = rememberTextFieldState(),
            onEvent = {},
            onGameClick = {},
            onProfileClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenInitialWithHistoryPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                history = SearchHistoryUiModel(
                    queries = listOf("The Witcher", "Cyberpunk 2077"),
                    games = listOf(
                        GameItemUiModel.getDummy(),
                        GameItemUiModel.getDummy()
                            .copy(id = 2, name = "Cyberpunk 2077")
                    )
                )
            ),
            textFieldState = rememberTextFieldState(),
            onEvent = {},
            onGameClick = {},
            onProfileClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenLoadingPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                contentState = SearchContentState.Loading
            ),
            textFieldState = rememberTextFieldState(),
            onEvent = {},
            onGameClick = {},
            onProfileClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenInitialPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(),
            textFieldState = rememberTextFieldState(),
            onEvent = {},
            onGameClick = {},
            onProfileClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenEmptyPreview() {
    GamesWishlistTheme {
        SearchScreenContent(
            uiState = SearchUiState(
                contentState = SearchContentState.Empty
            ),
            textFieldState = rememberTextFieldState(),
            onEvent = {},
            onGameClick = {},
            onProfileClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
