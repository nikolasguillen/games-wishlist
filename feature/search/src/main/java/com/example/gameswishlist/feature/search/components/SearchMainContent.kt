package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.model.DiscoverContentState
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchUiEvent

/**
 * The screen's single content area, shared by two independent contents. [contentState] decides which:
 * [SearchContentState.Idle] means no search is running, so the area belongs to [discoverState].
 */
@Composable
internal fun SearchMainContent(
    contentState: SearchContentState,
    discoverState: DiscoverContentState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    discoverListState: LazyListState = rememberLazyListState()
) {
    Box(modifier = modifier) {
        when (contentState) {
            is SearchContentState.Idle -> DiscoverContent(
                state = discoverState,
                onGameClick = onGameClick,
                listState = discoverListState
            )
            is SearchContentState.Error -> ErrorPage(message = contentState.message)
            is SearchContentState.Empty -> EmptySearchPlaceholder()
            is SearchContentState.Loading -> LoadingPage()
            is SearchContentState.Success -> {
                SearchResultGrid(
                    games = contentState.games,
                    activeFilters = contentState.activeFilters,
                    onFilterClick = { onEvent(SearchUiEvent.OnFilterClick(it)) },
                    onGameClick = onGameClick,
                    state = gridState
                )
            }
        }
    }
}

@Composable
private fun DiscoverContent(
    state: DiscoverContentState,
    onGameClick: (Int) -> Unit,
    listState: LazyListState
) {
    when (state) {
        is DiscoverContentState.Loading -> LoadingPage()
        is DiscoverContentState.Error -> ErrorPage(message = state.message)
        is DiscoverContentState.Content -> DiscoverFeed(
            hero = state.hero,
            popular = state.popular,
            upcoming = state.upcoming,
            onGameClick = onGameClick,
            recommended = state.recommended,
            state = listState
        )
    }
}

@Composable
private fun SearchMainContentPreview(
    contentState: SearchContentState,
    discoverState: DiscoverContentState = DiscoverContentState.Loading
) {
    GamesWishlistTheme {
        SearchMainContent(
            contentState = contentState,
            discoverState = discoverState,
            onEvent = {},
            onGameClick = {},
            gridState = rememberLazyGridState()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchMainContentDiscoverPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Idle,
        discoverState = DiscoverContentState.Content(
            hero = GameItemUiModel.getDummy().copy(id = 3, name = "Hollow Knight: Silksong"),
            popular = listOf(GameItemUiModel.getDummy()),
            upcoming = listOf(GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"))
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchMainContentDiscoverErrorPreview() {
    SearchMainContentPreview(
        contentState = SearchContentState.Idle,
        discoverState = DiscoverContentState.Error(
            UiText.DynamicString("Could not load the feed.")
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchMainContentLoadingPreview() {
    SearchMainContentPreview(SearchContentState.Loading)
}

@Preview(showBackground = true)
@Composable
private fun SearchMainContentEmptyPreview() {
    SearchMainContentPreview(SearchContentState.Empty)
}

@Preview(showBackground = true)
@Composable
private fun SearchMainContentErrorPreview() {
    SearchMainContentPreview(
        SearchContentState.Error(UiText.DynamicString("No internet connection."))
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchMainContentSuccessPreview() {
    SearchMainContentPreview(
        SearchContentState.Success(
            games = listOf(
                GameItemUiModel.getDummy(),
                GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077")
            ),
            filters = listOf(
                GameFilterUiModel.Platform(id = 0, label = UiText.DynamicString("PC"), selected = true)
            )
        )
    )
}
