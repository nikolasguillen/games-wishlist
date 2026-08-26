package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.component.ProfileIconButton
import com.example.gameswishlist.core.ui.component.RecentGameCard
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.annotatedStringResource
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.GameSuggestionUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchHistoryUiModel
import com.example.gameswishlist.feature.search.model.SearchSuggestionsUiModel
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import com.example.gameswishlist.core.ui.R as CoreUiR

/** Rows of shimmer while the debounced fetch runs. The remote call caps the real rows at four. */
private const val LOADING_SUGGESTION_COUNT = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTopBar(
    uiState: SearchUiState,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    scrollBehavior: SearchBarScrollBehavior,
    onSearch: (String) -> Unit,
    onGameClick: (Int) -> Unit,
    onEvent: (SearchUiEvent) -> Unit,
    onProfileClick: () -> Unit,
    backgroundColor: Color
) {
    val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
        searchBarColors = SearchBarDefaults.containedColors(state = searchBarState).copy(
            dividerColor = SearchBarDefaults.colors().dividerColor.copy(alpha = 0.5f)
        ),
        appBarContainerColor = Color.Transparent,
        scrolledAppBarContainerColor = Color.Transparent,
    )

    // Create a proxy so the internal SearchBar sees the scroll state (for colors)
    // but doesn't apply the scroll modifier, as we apply it to the outer Surface ourselves.
    val proxyScrollBehavior = remember(scrollBehavior) {
        object : SearchBarScrollBehavior by scrollBehavior {
            override fun Modifier.searchBarScrollBehavior(): Modifier = this
        }
    }

    val inputField = @Composable {
        SearchInputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { onSearch(textFieldState.text.toString()) },
            onClearSearch = { onEvent(SearchUiEvent.OnClearSearch) }
        )
    }

    Surface(
        color = backgroundColor,
        modifier = with(scrollBehavior) { Modifier.searchBarScrollBehavior() }
    ) {
        Column {
            CollapsedSearchBar(
                searchBarState = searchBarState,
                scrollBehavior = proxyScrollBehavior,
                appBarWithSearchColors = appBarWithSearchColors,
                inputField = inputField,
                onProfileClick = onProfileClick
            )

            val state = uiState.contentState
            if (state is SearchContentState.Success) {
                SearchSubHeader(
                    resultsCount = state.games.size,
                    isSortActive = uiState.sortBottomSheetState.isSortActive,
                    onOpenSort = { onEvent(SearchUiEvent.OnOpenSort) },
                    onOpenFilters = { onEvent(SearchUiEvent.OnOpenFilters) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }
        }
    }

    ExpandedSearchBar(
        searchBarState = searchBarState,
        inputField = inputField,
        history = uiState.history,
        suggestions = uiState.suggestions,
        searchQuery = textFieldState.text.toString(),
        appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
            searchBarColors = appBarWithSearchColors.searchBarColors,
            appBarContainerColor = Color.Transparent,
            scrolledAppBarContainerColor = MaterialTheme.appColors.searchBarScrolledContainerColor
        ),
        onCommitSearch = onSearch,
        onGameClick = onGameClick,
        onRemoveRecentGame = { onEvent(SearchUiEvent.OnRecentGameRemoved(it)) },
        onClearRecentSearches = { onEvent(SearchUiEvent.OnClearHistory) },
        onRemoveRecentSearchItem = { onEvent(SearchUiEvent.OnHistoryItemRemoved(it)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchInputField(
    textFieldState: TextFieldState,
    searchBarState: SearchBarState,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit
) {
    val searchInputFieldColor = if (searchBarState.currentValue == SearchBarValue.Collapsed) {
        MaterialTheme.appColors.searchBarInputFieldColor
    } else {
        MaterialTheme.appColors.expandedSearchBarColor
    }

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = searchBarState,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = searchInputFieldColor,
            unfocusedContainerColor = searchInputFieldColor
        ),
        onSearch = { onSearch() },
        readOnly = searchBarState.currentValue == SearchBarValue.Collapsed,
        placeholder = {
            Text(
                modifier = Modifier.clearAndSetSemantics {},
                text = stringResource(R.string.search_placeholder)
            )
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Always goes through onClearSearch, expanded or collapsed: an emptied field
                // means there is no committed query left, so the content behind the overlay must
                // fall back to the feed too, or collapsing with a blank field strands the user on
                // stale results with no way back (there is nothing left to show the clear button on).
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_search_content_description)
                        )
                    }
                }
                IconButton(onClick = onSearch) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search_content_description)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollapsedSearchBar(
    searchBarState: SearchBarState,
    scrollBehavior: SearchBarScrollBehavior,
    appBarWithSearchColors: AppBarWithSearchColors,
    inputField: @Composable () -> Unit,
    onProfileClick: () -> Unit
) {
    AppBarWithSearch(
        scrollBehavior = scrollBehavior,
        state = searchBarState,
        colors = appBarWithSearchColors,
        inputField = inputField,
        actions = { ProfileIconButton(onClick = onProfileClick) },
        contentPadding = PaddingValues(top = MaterialTheme.spacing.large)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpandedSearchBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    history: SearchHistoryUiModel,
    suggestions: SearchSuggestionsUiModel,
    searchQuery: String,
    appBarWithSearchColors: AppBarWithSearchColors,
    onCommitSearch: (String) -> Unit,
    onGameClick: (Int) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    onClearRecentSearches: () -> Unit,
    onRemoveRecentSearchItem: (query: String) -> Unit
) {
    var queryToRemove by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = appBarWithSearchColors.searchBarColors.copy(
            containerColor = MaterialTheme.appColors.expandedSearchBarColor
        )
    ) {
        // Typing owns the whole overlay: recent activity is what fills it before the first keystroke,
        // and once there is a query every row either resolves it or commits it.
        when {
            searchQuery.isNotBlank() -> SuggestionsSection(
                query = searchQuery,
                suggestions = suggestions,
                onCommitSearch = onCommitSearch,
                onGameSuggestionClick = onGameClick
            )

            history.isEmpty -> Text(
                text = stringResource(R.string.expanded_search_initial_message),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(MaterialTheme.spacing.large)
                    .fillMaxWidth()
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
            ) {
                if (history.queries.isNotEmpty()) {
                    RecentSearchesSection(
                        recentSearches = history.queries,
                        onClearRecentSearches = { showClearHistoryDialog = true },
                        onHistoryItemClicked = onCommitSearch,
                        onShowRemovalDialog = { queryToRemove = it }
                    )
                }
                if (history.games.isNotEmpty()) {
                    RecentGamesSection(
                        recentGames = history.games,
                        onGameClick = onGameClick,
                        onRemoveClick = onRemoveRecentGame
                    )
                }
            }
        }

        queryToRemove?.let { query ->
            CustomAlertDialog(
                title = stringResource(R.string.remove_history_item),
                message = annotatedStringResource(
                    R.string.remove_history_item_message,
                    query
                ),
                confirmButtonText = stringResource(CoreUiR.string.proceed_label),
                onConfirm = {
                    onRemoveRecentSearchItem(query)
                    queryToRemove = null
                },
                dismissButtonText = stringResource(CoreUiR.string.cancel),
                onDismiss = { queryToRemove = null }
            )
        }

        if (showClearHistoryDialog) {
            CustomAlertDialog(
                title = stringResource(R.string.clear_history_title),
                message = stringResource(R.string.clear_history_message),
                confirmButtonText = stringResource(CoreUiR.string.proceed_label),
                onConfirm = {
                    onClearRecentSearches()
                    showClearHistoryDialog = false
                },
                dismissButtonText = stringResource(CoreUiR.string.cancel),
                onDismiss = { showClearHistoryDialog = false }
            )
        }
    }
}

@Composable
private fun SuggestionsSection(
    query: String,
    suggestions: SearchSuggestionsUiModel,
    onCommitSearch: (String) -> Unit,
    onGameSuggestionClick: (Int) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = MaterialTheme.spacing.medium)) {
        if (suggestions.historySuggestions.isNotEmpty()) {
            item(key = "header_recent") {
                SuggestionSectionHeader(
                    title = stringResource(R.string.suggestions_section_recent)
                )
            }
            items(
                items = suggestions.historySuggestions,
                key = { "hist_$it" }
            ) { historyQuery ->
                HistorySuggestionRow(
                    query = historyQuery,
                    onClick = onCommitSearch,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (suggestions.isLoadingRemote || suggestions.gameSuggestions.isNotEmpty()) {
            item(key = "header_games") {
                SuggestionSectionHeader(
                    title = stringResource(R.string.suggestions_section_games)
                )
            }
        }

        if (suggestions.isLoadingRemote) {
            items(LOADING_SUGGESTION_COUNT, key = { "loading_$it" }) {
                LoadingSuggestionRow(modifier = Modifier.fillMaxWidth())
            }
        } else {
            items(
                items = suggestions.gameSuggestions,
                key = { "game_${it.id}" }
            ) { gameSuggestion ->
                GameSuggestionRow(
                    suggestion = gameSuggestion,
                    onClick = onGameSuggestionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item(key = "commit_search") {
            SeeAllResultsRow(
                query = query,
                onClick = onCommitSearch,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onClearRecentSearches: () -> Unit,
    onHistoryItemClicked: (String) -> Unit,
    onShowRemovalDialog: (String) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large)
        ) {
            Text(
                text = stringResource(R.string.recent_searches),
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(
                onClick = onClearRecentSearches
            ) {
                Text(stringResource(R.string.clear_all))
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
        ) {
            items(items = recentSearches, key = { it.hashCode() }) { recentSearch ->
                val inputChipInteractionSource = remember { MutableInteractionSource() }
                Box {
                    SuggestionChip(
                        onClick = { onHistoryItemClicked(recentSearch) },
                        label = {
                            Text(
                                text = recentSearch,
                                maxLines = 1
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null
                            )
                        },
                        contentPadding = PaddingValues(all = MaterialTheme.spacing.small),
                        interactionSource = inputChipInteractionSource
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .combinedClickable(
                                onLongClick = { onShowRemovalDialog(recentSearch) },
                                onClick = { onHistoryItemClicked(recentSearch) },
                                interactionSource = inputChipInteractionSource,
                                indication = null
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentGamesSection(
    recentGames: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
        Text(
            text = stringResource(R.string.recently_viewed),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
        ) {
            items(items = recentGames, key = { it.id }) { game ->
                RecentGameCard(
                    game = game,
                    onClick = { onGameClick(game.id) },
                    onRemoveClick = { onRemoveClick(game.id) }
                )
            }
        }
    }
}

private val previewHistory = SearchHistoryUiModel(
    queries = listOf("The Witcher", "Cyberpunk 2077"),
    games = listOf(
        GameItemUiModel.getDummy(),
        GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
    )
)

/** The overlay at its tallest: three recent queries, the four capped games and the commit row. */
private val previewSuggestions = SearchSuggestionsUiModel(
    historySuggestions = listOf("cyberpunk", "cyberpunk mods", "cyberpunk 2078"),
    gameSuggestions = listOf(
        GameSuggestionUiModel(1, "Cyberpunk 2077", null, "CD Projekt Red · 2020"),
        GameSuggestionUiModel(2, "Cyberpunk 2077: Phantom Liberty", null, "CD Projekt Red · 2023"),
        GameSuggestionUiModel(3, "Cyberpunk 2077: Ultimate Edition", null, "CD Projekt Red · 2023"),
        GameSuggestionUiModel(4, "Cyberpunk: Edgerunners", null, "Studio Trigger")
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun SearchTopBarPreview() {
    GamesWishlistTheme {
        SearchTopBar(
            uiState = SearchUiState(
                history = previewHistory,
                contentState = SearchContentState.Success(
                    games = listOf(GameItemUiModel.getDummy()),
                    filters = listOf(
                        GameFilterUiModel.Platform(
                            id = 0,
                            label = UiText.DynamicString("PC"),
                            selected = true
                        )
                    )
                )
            ),
            searchBarState = rememberContainedSearchBarState(),
            textFieldState = rememberTextFieldState("The Witcher"),
            scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
            onSearch = {},
            onGameClick = {},
            onEvent = {},
            onProfileClick = {},
            backgroundColor = Color.Transparent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun SearchInputFieldPreview() {
    GamesWishlistTheme {
        SearchInputField(
            textFieldState = rememberTextFieldState("The Witcher"),
            searchBarState = rememberContainedSearchBarState(),
            onSearch = {},
            onClearSearch = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CollapsedSearchBarPreview() {
    GamesWishlistTheme {
        val searchBarState = rememberContainedSearchBarState()
        val textFieldState = rememberTextFieldState("The Witcher")

        CollapsedSearchBar(
            searchBarState = searchBarState,
            scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
            appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            onProfileClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ExpandedSearchBarPreview() {
    GamesWishlistTheme {
        val searchBarState = rememberContainedSearchBarState(SearchBarValue.Expanded)
        val textFieldState = rememberTextFieldState()

        ExpandedSearchBar(
            searchBarState = searchBarState,
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            history = previewHistory,
            suggestions = SearchSuggestionsUiModel(),
            searchQuery = "",
            appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
            onCommitSearch = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onRemoveRecentSearchItem = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ExpandedSearchBarTypingPreview() {
    GamesWishlistTheme {
        val searchBarState = rememberContainedSearchBarState(SearchBarValue.Expanded)
        val textFieldState = rememberTextFieldState("cyberpunk")

        ExpandedSearchBar(
            searchBarState = searchBarState,
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            history = previewHistory,
            suggestions = previewSuggestions,
            searchQuery = "cyberpunk",
            appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
            onCommitSearch = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onRemoveRecentSearchItem = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ExpandedSearchBarLoadingPreview() {
    GamesWishlistTheme {
        val searchBarState = rememberContainedSearchBarState(SearchBarValue.Expanded)
        val textFieldState = rememberTextFieldState("cyberpunk")

        ExpandedSearchBar(
            searchBarState = searchBarState,
            inputField = {
                SearchInputField(
                    textFieldState = textFieldState,
                    searchBarState = searchBarState,
                    onSearch = {},
                    onClearSearch = {}
                )
            },
            history = previewHistory,
            suggestions = previewSuggestions.copy(
                gameSuggestions = emptyList(),
                isLoadingRemote = true
            ),
            searchQuery = "cyberpunk",
            appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(),
            onCommitSearch = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onRemoveRecentSearchItem = {}
        )
    }
}
