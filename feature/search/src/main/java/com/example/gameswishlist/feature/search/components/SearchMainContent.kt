package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.core.ui.component.LoadingPage
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchUiEvent

@Composable
internal fun SearchMainContent(
    contentState: SearchContentState,
    onEvent: (SearchUiEvent) -> Unit,
    onGameClick: (Int) -> Unit,
    gridState: LazyStaggeredGridState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (contentState) {
            is SearchContentState.Error -> ErrorPage(message = contentState.message)
            is SearchContentState.Initial -> InitialSearchPlaceholder()
            is SearchContentState.Empty -> EmptySearchPlaceholder()
            is SearchContentState.Loading -> LoadingPage()
            is SearchContentState.Success -> {
                SearchResultGrid(
                    games = contentState.games,
                    activeFilters = contentState.activeFilters,
                    onFilterClick = { onEvent(SearchUiEvent.OnFilterClick(it)) },
                    onGameClick = onGameClick,
                    state = gridState,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                )
            }
        }
    }
}
