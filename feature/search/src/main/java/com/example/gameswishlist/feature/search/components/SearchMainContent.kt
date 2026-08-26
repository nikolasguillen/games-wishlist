package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchUiEvent

@Composable
internal fun SearchMainContent(
    contentState: SearchContentState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (contentState) {
            is SearchContentState.Error -> ErrorPage(message = contentState.message)
            is SearchContentState.Discover -> DiscoverPlaceholder()
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
private fun SearchMainContentPreview(contentState: SearchContentState) {
    GamesWishlistTheme {
        SearchMainContent(
            contentState = contentState,
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
        SearchContentState.Discover(
            popular = listOf(GameItemUiModel.getDummy()),
            upcoming = listOf(GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"))
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
