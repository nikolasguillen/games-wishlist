@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gameswishlist.feature.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.core.ui.component.VerticalGameCard
import com.example.gameswishlist.core.ui.model.GameItem
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import kotlinx.coroutines.launch
import com.example.gameswishlist.feature.search.R as SearchR

@Composable
fun SearchScreen(
    viewModel: SearchViewModel, onGameClick: (Int) -> Unit, modifier: Modifier = Modifier
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
    val textFieldState = rememberTextFieldState()
    val searchBarState = rememberContainedSearchBarState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val searchInputFieldColor = if (searchBarState.currentValue == SearchBarValue.Collapsed) {
        MaterialTheme.appColors.searchBarInputFieldColor
    } else {
        MaterialTheme.appColors.expandedSearchBarColor
    }

    val appBarWithSearchColors =
        SearchBarDefaults.appBarWithSearchColors(
            searchBarColors = SearchBarDefaults.containedColors(state = searchBarState).copy(
                inputFieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = searchInputFieldColor,
                    unfocusedContainerColor = searchInputFieldColor
                ),
                dividerColor = SearchBarDefaults.colors().dividerColor.copy(alpha = 0.5f)
            ),
            appBarContainerColor = MaterialTheme.appColors.appBackground,
            scrolledAppBarContainerColor = MaterialTheme.appColors.searchBarScrolledContainerColor,
        )

    fun onSearch(query: String = textFieldState.text.toString()) {
        scope.launch {
            searchBarState.animateToCollapsed()
        }
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
        onEvent(SearchUiEvent.OnSearchTriggered(query))
    }

    val inputField =
        @Composable {
            SearchBarDefaults.InputField(
                textFieldState = textFieldState,
                searchBarState = searchBarState,
                colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
                onSearch = { onSearch() },
                readOnly = searchBarState.currentValue == SearchBarValue.Collapsed,
                placeholder = {
                    Text(
                        modifier = Modifier.clearAndSetSemantics {},
                        text = stringResource(SearchR.string.search_placeholder)
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { onSearch() }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    }
                }
            )
        }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.appColors.appBackground,
        topBar = {
            CollapsedSearchBar(
                searchBarState = searchBarState,
                scrollBehavior = scrollBehavior,
                appBarWithSearchColors = appBarWithSearchColors,
                inputField = inputField
            )
            ExpandedSearchBar(
                searchBarState = searchBarState,
                inputField = inputField,
                recentSearches = uiState.recentSearches,
                appBarWithSearchColors = appBarWithSearchColors,
                onHistoryItemClicked = { onSearch(it) },
                onClearRecentSearches = { onEvent(SearchUiEvent.OnClearHistory) },
                onRemoveRecentSearchItem = { onEvent(SearchUiEvent.OnHistoryItemRemoved(it)) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MaterialTheme.spacing.large),
        ) {
            when (val state = uiState.contentState) {
                is SearchContentState.Loading -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(70.dp))
                    }
                }

                is SearchContentState.Error -> {
                    Box { ErrorPage(message = state.message) }
                }

                is SearchContentState.Initial -> {
                    Box { InitialSearchPlaceholder() }
                }

                is SearchContentState.Empty -> {
                    Box { EmptySearchPlaceholder() }
                }

                is SearchContentState.Success -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
                        verticalItemSpacing = MaterialTheme.spacing.extraLarge,
                        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.large)
                    ) {
                        items(state.games) { game ->
                            VerticalGameCard(game = game, onClick = { onGameClick(game.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedSearchBar(
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
            bottom = MaterialTheme.spacing.large,
            top = WindowInsets.statusBars.asPaddingValues()
                .calculateTopPadding() + MaterialTheme.spacing.large,
            start = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium
        )
    )
}

@Composable
private fun ExpandedSearchBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    recentSearches: List<String>,
    appBarWithSearchColors: AppBarWithSearchColors,
    onHistoryItemClicked: (query: String) -> Unit,
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
        if (recentSearches.isEmpty()) return@ExpandedFullScreenSearchBar
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
                )
        ) {
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
                    Text(
                        text = stringResource(SearchR.string.clear_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.appColors.onAppBackground
                    )
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
                                    onLongClick = { showRecentSearchRemovalDialog(recentSearch) },
                                    onClick = { onHistoryItemClicked(recentSearch) },
                                    interactionSource = inputChipInteractionSource,
                                    indication = null,
                                )
                        )
                    }
                }
            }
        }

        if (showHistoryItemRemovalDialog) {
            CustomAlertDialog(
                title = stringResource(SearchR.string.remove_history_item),
                message = stringResource(
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
private fun InitialSearchPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = Icons.Outlined.SportsEsports,
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.height(100.dp)
        )
        Text(
            text = stringResource(SearchR.string.search_initial_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptySearchPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = Icons.Outlined.SmartToy,
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.height(100.dp)
        )
        Text(
            text = stringResource(SearchR.string.search_no_results),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                        GameItem.getDummy(),
                        GameItem.getDummy().copy(id = 2, name = "The Witcher 2")
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
fun SearchScreenInitialPreview() {
    GamesWishlistTheme {
        SearchScreenContent(uiState = SearchUiState(), onEvent = {}, onGameClick = {})
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
