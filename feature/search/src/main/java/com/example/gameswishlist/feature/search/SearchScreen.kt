@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gameswishlist.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.components.CollapsedSearchBar
import com.example.gameswishlist.feature.search.components.EmptySearchPlaceholder
import com.example.gameswishlist.feature.search.components.ExpandedSearchBar
import com.example.gameswishlist.feature.search.components.InitialSearchPlaceholder
import com.example.gameswishlist.feature.search.components.SearchResultGrid
import com.example.gameswishlist.feature.search.components.SearchSkeletonGrid
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
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

    val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
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
        scope.launch { searchBarState.animateToCollapsed() }
        if (textFieldState.text.toString() != query) {
            textFieldState.setTextAndPlaceCursorAtEnd(query)
        }
        onEvent(SearchUiEvent.OnSearchTriggered(query))
    }

    val inputField = @Composable {
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
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
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
        val paddingModifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState.contentState) {
                is SearchContentState.Loading -> SearchSkeletonGrid()
                is SearchContentState.Error -> ErrorPage(
                    message = state.message,
                    modifier = paddingModifier
                )

                is SearchContentState.Initial -> InitialSearchPlaceholder(modifier = paddingModifier)
                is SearchContentState.Empty -> EmptySearchPlaceholder(modifier = paddingModifier)
                is SearchContentState.Success -> SearchResultGrid(
                    games = state.games,
                    filters = state.filters,
                    onGameClick = onGameClick,
                    onFilterClick = { onEvent(SearchUiEvent.OnFilterClick(it)) },
                    modifier = paddingModifier
                )
            }
        }
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
