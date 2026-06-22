@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gameswishlist.feature.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.components.SearchFilterBottomSheet
import com.example.gameswishlist.feature.search.components.SearchMainContent
import com.example.gameswishlist.feature.search.components.SearchSortBottomSheet
import com.example.gameswishlist.feature.search.components.SearchTopBar
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import kotlinx.coroutines.launch

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
    val searchBarState = rememberContainedSearchBarState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    val textFieldState = rememberTextFieldState()
    val gridState = rememberLazyStaggeredGridState()

    val showScrollToTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 3
        }
    }

    val onSearch: (String) -> Unit = { query ->
        textFieldState.setTextAndPlaceCursorAtEnd(query)
        scope.launch { searchBarState.animateToCollapsed() }
        onEvent(SearchUiEvent.OnSearchTriggered(query))
    }

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
                onEvent = onEvent
            )
        },
        floatingActionButton = {
            if (showScrollToTop) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            // Reset search bar scroll state to change background color
                            scrollBehavior.contentOffset = 0f
                            scrollBehavior.scrollOffset = 0f
                            // Scroll the grid to top
                            gridState.animateScrollToItem(0)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    ) { innerPadding ->
        SearchMainContent(
            contentState = uiState.contentState,
            onEvent = onEvent,
            onGameClick = onGameClick,
            gridState = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )

        SearchFilterBottomSheet(
            state = uiState.filtersBottomSheetState,
            onEvent = onEvent
        )

        SearchSortBottomSheet(
            state = uiState.sortBottomSheetState,
            onEvent = onEvent
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
