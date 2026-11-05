package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.nikolasguillen.questlog.core.ui.component.ListSelectorSheet
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFab
import com.nikolasguillen.questlog.core.ui.component.StatusBarProtection
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.mapper.searchDropdownItems
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchUiEvent
import com.nikolasguillen.questlog.feature.search.model.SearchUiState
import kotlinx.coroutines.launch

/**
 * The Search screen arranged for a window at least 840dp wide, which is where the results and the filters will get
 * their own wide arrangement too: this is the one place the wide Search screen is put together.
 *
 * It takes the same parameters as [CompactSearchLayout] so [SearchScreenContent] can dispatch between them with the
 * same arguments, and keeps the state they share (the search text, the dialogs) in the screen, so a window that
 * crosses 840dp does not lose it.
 *
 * @param onGameClick Already wrapped by the screen to collapse the search bar behind the opened page.
 * @param historyDialogs Which recent-search dialog is open. The screen owns it, so it survives a switch between the
 * two layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WideSearchLayout(
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
    val query = textFieldState.text.toString()
    val items = remember(query, uiState.browseGenres, uiState.history, uiState.suggestions) {
        searchDropdownItems(query, uiState.browseGenres, uiState.history, uiState.suggestions)
    }

    // The highlight is one item, not a position, so it follows its row when the list changes. It is plain state, not
    // saved: crossing the 840dp threshold may reset it. A highlight whose item is gone is dropped in the same frame
    // (the reconcile on the read) and then forgotten for good (the effect).
    var highlightedItem by remember { mutableStateOf<SearchDropdownItemUiModel?>(null) }
    val highlighted = reconcileHighlight(items, highlightedItem)
    LaunchedEffect(items) { highlightedItem = reconcileHighlight(items, highlightedItem) }
    LaunchedEffect(searchBarState.targetValue) {
        if (searchBarState.targetValue == SearchBarValue.Collapsed) highlightedItem = null
    }

    val scope = rememberCoroutineScope()

    val activate: (SearchDropdownItemUiModel) -> Unit = {
        activateDropdownItem(
            item = it,
            uiState = uiState,
            textFieldState = textFieldState,
            historyDialogs = historyDialogs,
            onEvent = onEvent,
            onSearch = onSearch,
            onBrowseGenre = onBrowseGenre,
            onGameClick = onGameClick
        )
    }

    // The text field keeps keyboard focus while the panel is open, so the arrow keys are read on the field itself and
    // move a highlight. Only an open panel is handled; every other key (typing, the caret keys, select all, copy,
    // paste) is left to the field, and with the panel closed Material's own open-on-Down applies.
    val keyHandler = Modifier.onPreviewKeyEvent { event ->
        if (searchBarState.targetValue != SearchBarValue.Expanded) return@onPreviewKeyEvent false
        val isDown = event.key == Key.DirectionDown || event.key == Key.NumPadDirectionDown
        val isUp = event.key == Key.DirectionUp || event.key == Key.NumPadDirectionUp
        when (event.type) {
            // Material's own handler on the field moves focus down on any event of the Down key, the release
            // included, so the release is swallowed too: otherwise focus would leave the field right after the first
            // press and no later key would reach this handler.
            KeyEventType.KeyUp -> isDown || isUp

            KeyEventType.KeyDown -> when {
                // Consumed even on the last item, so focus never moves out of the field.
                isDown -> {
                    highlightedItem = highlightNext(items, highlighted)
                    true
                }

                isUp -> {
                    highlightedItem = highlightPrevious(items, highlighted)
                    true
                }

                // With nothing highlighted the field's own search action commits what is typed.
                event.key == Key.Enter || event.key == Key.NumPadEnter -> highlighted?.let {
                    activate(it)
                    true
                } ?: false

                event.key == Key.Delete && event.isShiftPressed ->
                    removeHighlighted(highlighted, historyDialogs, onEvent)

                // Closes the panel and leaves the typed text alone. Consumed, so it never reaches the back handling
                // that would clear a committed search.
                event.key == Key.Escape -> {
                    scope.launch { searchBarState.animateToCollapsed() }
                    true
                }

                // Leaving the field by keyboard closes the panel too. Not consumed: focus moves on from the bar
                // wherever the platform allows it.
                event.key == Key.Tab -> {
                    scope.launch { searchBarState.animateToCollapsed() }
                    false
                }

                else -> false
            }

            else -> false
        }
    }

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
                // Material measures the field inside the popup with loose constraints, so without this it would
                // shrink to its own width the moment the panel opens, instead of staying as wide as the bar.
                inputFieldModifier = Modifier.fillMaxWidth().then(keyHandler),
                readOnlyWhenCollapsed = false,
                expandedContent = { inputField, colors, shape ->
                    WideSearchDropdown(
                        searchBarState = searchBarState,
                        inputField = inputField,
                        searchBarShape = shape,
                        searchBarColors = colors,
                        uiState = uiState,
                        query = query,
                        onActivate = activate,
                        onRemoveRecentGame = { onEvent(SearchUiEvent.OnRecentGameRemoved(it)) },
                        historyDialogs = historyDialogs,
                        onConfirmRemoveQuery = { onEvent(SearchUiEvent.OnHistoryItemRemoved(it)) },
                        onConfirmClearHistory = { onEvent(SearchUiEvent.OnClearHistory) },
                        highlighted = highlighted,
                        itemModifier = { item ->
                            Modifier.searchPanelItem(item, highlighted, onHighlight = { highlightedItem = it })
                        }
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
                WideDiscoverFeed(
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

/**
 * Does what choosing [item] in the dropdown means, the same as choosing the same thing in the full-screen overlay:
 * a type chip changes the type and leaves the panel open, a genre starts a browse, a recent or suggested search runs,
 * a game opens, "Clear all" asks for confirmation, and "See all results" commits what is typed.
 */
private fun activateDropdownItem(
    item: SearchDropdownItemUiModel,
    uiState: SearchUiState,
    textFieldState: TextFieldState,
    historyDialogs: SearchHistoryDialogState,
    onEvent: (SearchUiEvent) -> Unit,
    onSearch: (String) -> Unit,
    onBrowseGenre: (BrowseGenreUiModel) -> Unit,
    onGameClick: (Int) -> Unit
) {
    when (item) {
        is SearchDropdownItemUiModel.TypeChip -> onEvent(SearchUiEvent.OnSearchTypeSelected(item.type))
        is SearchDropdownItemUiModel.Genre ->
            uiState.browseGenres.firstOrNull { it.id == item.genreId }?.let(onBrowseGenre)
        SearchDropdownItemUiModel.ClearHistory -> historyDialogs.showClearHistoryDialog = true
        is SearchDropdownItemUiModel.RecentQuery -> onSearch(item.query)
        is SearchDropdownItemUiModel.HistorySuggestion -> onSearch(item.query)
        is SearchDropdownItemUiModel.RecentGame -> onGameClick(item.gameId)
        is SearchDropdownItemUiModel.GameSuggestion -> onGameClick(item.gameId)
        SearchDropdownItemUiModel.SeeAllResults -> onSearch(textFieldState.text.toString())
    }
}

/**
 * Shift+Delete on the highlighted item: a recent search asks to be removed (through the same confirmation as a long
 * press) and a recently viewed game is removed. Returns whether the key was used; on any other item, or on none, it is
 * left alone.
 */
private fun removeHighlighted(
    highlighted: SearchDropdownItemUiModel?,
    historyDialogs: SearchHistoryDialogState,
    onEvent: (SearchUiEvent) -> Unit
): Boolean = when (highlighted) {
    is SearchDropdownItemUiModel.RecentQuery -> {
        historyDialogs.queryToRemove = highlighted.query
        true
    }

    is SearchDropdownItemUiModel.RecentGame -> {
        onEvent(SearchUiEvent.OnRecentGameRemoved(highlighted.gameId))
        true
    }

    else -> false
}
