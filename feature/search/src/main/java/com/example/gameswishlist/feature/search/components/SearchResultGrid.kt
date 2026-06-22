package com.example.gameswishlist.feature.search.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.VerticalGameCard
import com.example.gameswishlist.core.ui.component.VerticalGameCardSkeleton
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState

@Composable
fun SearchResultGrid(
    contentState: SearchContentState,
    onFilterClick: (GameFilterUiModel) -> Unit,
    onGameClick: (Int) -> Unit,
    state: LazyStaggeredGridState,
    modifier: Modifier = Modifier
) {
    val games = (contentState as? SearchContentState.Success)?.games ?: emptyList()
    val activeFilters = (contentState as? SearchContentState.Success)?.activeFilters ?: emptyList()
    val isLoading = contentState is SearchContentState.Loading

    LaunchedEffect(games) {
        if (games.isNotEmpty()) {
            state.scrollToItem(0)
        }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = state,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
        verticalItemSpacing = MaterialTheme.spacing.extraLarge,
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
        modifier = modifier
    ) {
        if (activeFilters.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ActiveFiltersRow(
                    filters = activeFilters,
                    onFilterClick = onFilterClick,
                    modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)
                )
            }
        }

        if (isLoading) {
            items(10) {
                VerticalGameCardSkeleton()
            }
        } else {
            items(items = games, key = { it.id }) { game ->
                VerticalGameCard(game = game, onClick = { onGameClick(game.id) })
            }
        }
    }
}

@Composable
fun ActiveFiltersRow(
    filters: List<GameFilterUiModel>,
    onFilterClick: (GameFilterUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
        modifier = modifier
    ) {
        items(
            items = filters,
            key = { filter -> "${filter::class.simpleName}:${filter.id}" }
        ) { gameFilter ->
            FilterChip(
                selected = true,
                onClick = { onFilterClick(gameFilter) },
                label = {
                    Text(text = gameFilter.label.asString(), maxLines = 1)
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                },
                contentPadding = PaddingValues(all = MaterialTheme.spacing.small),
                modifier = Modifier.animateContentSize()
            )
        }
    }
}
