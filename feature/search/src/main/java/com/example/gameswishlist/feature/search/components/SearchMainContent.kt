package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.ErrorPage
import com.example.gameswishlist.feature.search.model.SearchContentState
import androidx.compose.material3.MaterialTheme

@Composable
internal fun SearchMainContent(
    contentState: SearchContentState,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when (contentState) {
            is SearchContentState.Loading -> SearchSkeletonGrid()
            is SearchContentState.Error -> ErrorPage(message = contentState.message)
            is SearchContentState.Initial -> InitialSearchPlaceholder()
            is SearchContentState.Empty -> EmptySearchPlaceholder()
            is SearchContentState.Success -> SearchResultGrid(
                games = contentState.games,
                onGameClick = onGameClick,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
            )
        }
    }
}
