package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.nikolasguillen.questlog.core.ui.component.ListSelectorSheet
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFab
import com.nikolasguillen.questlog.core.ui.component.StatusBarProtection
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchUiEvent
import com.nikolasguillen.questlog.feature.search.model.SearchUiState

/**
 * The Search screen arranged for a compact window: the search bar opens into a full-screen overlay, and the
 * Discover feed is the single-column one.
 *
 * It shares everything else with [WideSearchLayout]; the two differ only in the `expandedContent` of the top bar
 * and in the `discoverFeed` of the main content. [SearchScreenContent] picks one of them, once.
 *
 * @param onGameClick Already wrapped by the screen to collapse the search bar behind the opened page.
 * @param historyDialogs Which recent-search dialog is open. The screen owns it, so it survives a switch between the
 * two layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactSearchLayout(
    uiState: SearchUiState,
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    scrollState: SearchScrollState,
    snackbarHostState: SnackbarHostState,
    backgroundColor: Color,
    historyDialogs: SearchHistoryDialogState,
    onEvent: (SearchUiEvent) -> Unit,
    onSearch: (String) -> Unit,
    onBrowseGenre: (BrowseGenreUiModel) -> Unit,
    onOpenShelf: (DiscoverShelfUiModel) -> Unit,
    onGameClick: (Int) -> Unit,
    onCharacterClick: (Int) -> Unit,
    onDeveloperClick: (Int) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.nestedScroll(scrollState.scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        // Rotated with the camera on the end edge, the last column would sit under it; the start side is already
        // consumed by the rail.
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SearchTopBar(
                uiState = uiState,
                searchBarState = searchBarState,
                textFieldState = textFieldState,
                scrollBehavior = scrollState.scrollBehavior,
                onSearch = onSearch,
                onEvent = onEvent,
                onProfileClick = onProfileClick,
                backgroundColor = backgroundColor,
                expandedContent = { inputField, colors, _ ->
                    ExpandedSearchBar(
                        searchBarState = searchBarState,
                        inputField = inputField,
                        history = uiState.history,
                        suggestions = uiState.suggestions,
                        browseGenres = uiState.browseGenres,
                        searchQuery = textFieldState.text.toString(),
                        selectedSearchType = uiState.selectedSearchType,
                        searchBarColors = colors,
                        onSearchTypeSelected = { onEvent(SearchUiEvent.OnSearchTypeSelected(it)) },
                        onCommitSearch = onSearch,
                        onBrowseGenre = onBrowseGenre,
                        onGameClick = onGameClick,
                        onRemoveRecentGame = { onEvent(SearchUiEvent.OnRecentGameRemoved(it)) },
                        onClearRecentSearches = { onEvent(SearchUiEvent.OnClearHistory) },
                        onRemoveRecentSearchItem = { onEvent(SearchUiEvent.OnHistoryItemRemoved(it)) },
                        historyDialogs = historyDialogs
                    )
                }
            )
        },
        floatingActionButton = {
            ScrollToTopFab(
                visible = scrollState.showScrollToTop,
                onClick = scrollState.onScrollToTop
            )
        }
    ) { innerPadding ->
        SearchMainContent(
            contentState = uiState.contentState,
            discoverState = uiState.discover,
            onEvent = onEvent,
            onGameClick = onGameClick,
            onCharacterClick = onCharacterClick,
            onDeveloperClick = onDeveloperClick,
            gridState = scrollState.gridState,
            activeBrowse = uiState.activeBrowse,
            resultsType = uiState.resultsType,
            discoverFeed = { content, listState ->
                DiscoverFeed(
                    hero = content.hero,
                    popular = content.popular,
                    upcoming = content.upcoming,
                    onGameClick = onGameClick,
                    onSaveClick = { onEvent(SearchUiEvent.OnToggleSave(it)) },
                    onLongClick = { onEvent(SearchUiEvent.OnOpenListSelector(it)) },
                    onSeeAllClick = onOpenShelf,
                    recommended = content.recommended,
                    isStale = content.isStale,
                    isRefreshing = content.isRefreshing,
                    onRefreshClick = { onEvent(SearchUiEvent.OnRefreshDiscover) },
                    onDismissRefreshClick = { onEvent(SearchUiEvent.OnDismissDiscoverRefresh) },
                    state = listState
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            discoverListState = scrollState.discoverListState
        )

        SearchFilterBottomSheet(state = uiState.filtersBottomSheetState, onEvent = onEvent)
        SearchSortBottomSheet(state = uiState.sortBottomSheetState, onEvent = onEvent)

        uiState.listSelectorState?.let { selectorState ->
            ListSelectorSheet(
                gameName = UiText.DynamicString(selectorState.gameName),
                list = selectorState.availableLists,
                onDismiss = { onEvent(SearchUiEvent.OnDismissListSelector) },
                onConfirm = { onEvent(SearchUiEvent.OnConfirmListSelection) },
                onToggleList = { listId -> onEvent(SearchUiEvent.OnToggleListSelection(listId)) }
            )
        }

        StatusBarProtection(color = backgroundColor)
    }
}
