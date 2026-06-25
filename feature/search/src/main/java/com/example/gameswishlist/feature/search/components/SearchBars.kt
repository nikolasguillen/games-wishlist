package com.example.gameswishlist.feature.search.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.component.RecentGameCard
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.util.annotatedStringResource
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchHistoryUiModel
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import com.example.gameswishlist.feature.search.R as SearchR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTopBar(
    uiState: SearchUiState,
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    scrollBehavior: SearchBarScrollBehavior,
    onSearch: (String) -> Unit,
    onGameClick: (Int) -> Unit,
    onEvent: (SearchUiEvent) -> Unit
) {
    val isScrolled by remember(scrollBehavior) {
        derivedStateOf {
            if (scrollBehavior.scrollOffsetLimit != 0f) {
                val fraction =
                    1 - ((scrollBehavior.scrollOffsetLimit - scrollBehavior.contentOffset).coerceIn(
                        scrollBehavior.scrollOffsetLimit,
                        0f
                    ) / scrollBehavior.scrollOffsetLimit)
                fraction > 0.01f
            } else false
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.appColors.searchBarScrolledContainerColor
        else MaterialTheme.appColors.appBackground,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "topBarBackground"
    )

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
            onSearch = { onSearch(textFieldState.text.toString()) }
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
                inputField = inputField
            )

            val state = uiState.contentState
            if (state is SearchContentState.Success) {
                SearchSubHeader(
                    uiState = uiState,
                    onEvent = onEvent,
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
        appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
            searchBarColors = appBarWithSearchColors.searchBarColors,
            appBarContainerColor = MaterialTheme.appColors.appBackground,
            scrolledAppBarContainerColor = MaterialTheme.appColors.searchBarScrolledContainerColor
        ),
        onHistoryItemClicked = { onSearch(it) },
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
    onSearch: () -> Unit
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
                text = stringResource(SearchR.string.search_placeholder)
            )
        },
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsedSearchBar(
    searchBarState: SearchBarState,
    scrollBehavior: SearchBarScrollBehavior,
    appBarWithSearchColors: AppBarWithSearchColors,
    inputField: @Composable () -> Unit
) {
    AppBarWithSearch(
        scrollBehavior = scrollBehavior,
        state = searchBarState,
        colors = appBarWithSearchColors,
        inputField = inputField,
        contentPadding = PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues()
                .calculateTopPadding() + MaterialTheme.spacing.large
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedSearchBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    history: SearchHistoryUiModel,
    appBarWithSearchColors: AppBarWithSearchColors,
    onHistoryItemClicked: (query: String) -> Unit,
    onGameClick: (Int) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    onClearRecentSearches: () -> Unit,
    onRemoveRecentSearchItem: (query: String) -> Unit
) {
    var showHistoryItemRemovalDialog by remember { mutableStateOf(false) }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = appBarWithSearchColors.searchBarColors.copy(
            containerColor = MaterialTheme.appColors.expandedSearchBarColor
        )
    ) {
        if (history.isEmpty) return@ExpandedFullScreenSearchBar // TODO mettere placeholder?
        var recentSearchToBeRemoved by remember { mutableStateOf("") }

        fun showRecentSearchRemovalDialog(itemToRemove: String) {
            recentSearchToBeRemoved = itemToRemove
            showHistoryItemRemovalDialog = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = MaterialTheme.spacing.medium
                ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
        ) {
            if (history.queries.isNotEmpty()) {
                RecentSearchesSection(
                    recentSearches = history.queries,
                    onClearRecentSearches = onClearRecentSearches,
                    onHistoryItemClicked = onHistoryItemClicked,
                    onShowRemovalDialog = ::showRecentSearchRemovalDialog
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

        if (showHistoryItemRemovalDialog) {
            CustomAlertDialog(
                title = stringResource(SearchR.string.remove_history_item),
                message = annotatedStringResource(
                    SearchR.string.remove_history_item_message,
                    recentSearchToBeRemoved
                ),
                confirmButtonText = stringResource(SearchR.string.proceed_label),
                onConfirm = { onRemoveRecentSearchItem(recentSearchToBeRemoved) },
                dismissButtonText = stringResource(SearchR.string.cancel),
                onDismiss = { showHistoryItemRemovalDialog = false }
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
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large)
        ) {
            Text(
                text = stringResource(SearchR.string.recent_searches),
                style = MaterialTheme.typography.titleMedium
            )

            TextButton(
                onClick = onClearRecentSearches
            ) {
                Text(stringResource(SearchR.string.clear_all))
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
        ) {
            items(items = recentSearches, key = { it }) { recentSearch ->
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
            text = stringResource(SearchR.string.recently_viewed),
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

