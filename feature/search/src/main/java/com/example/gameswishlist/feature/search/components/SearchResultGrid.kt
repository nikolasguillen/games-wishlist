package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomFilterChip
import com.example.gameswishlist.core.ui.component.VerticalGameCard
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.feature.search.model.GameFilterUiModel

@Composable
fun SearchResultGrid(
    games: List<GameItemUiModel>,
    activeFilters: List<GameFilterUiModel>,
    onFilterClick: (GameFilterUiModel) -> Unit,
    onGameClick: (Int) -> Unit,
    state: LazyStaggeredGridState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(games) {
        if (games.isNotEmpty()) {
            state.scrollToItem(0)
        }
    }

    if (games.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            if (activeFilters.isNotEmpty()) {
                ActiveFiltersRow(
                    filters = activeFilters,
                    onFilterClick = onFilterClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            NoFilteredResultsPlaceholder(modifier = Modifier.weight(1f))
        }
    } else {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
            verticalItemSpacing = MaterialTheme.spacing.large,
            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
            modifier = modifier
        ) {
            if (activeFilters.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ActiveFiltersRow(
                        filters = activeFilters,
                        onFilterClick = onFilterClick
                    )
                }
            }
            items(items = games, key = { it.id }) { game ->
                VerticalGameCard(
                    game = game,
                    onClick = { onGameClick(game.id) },
                    modifier = Modifier.animateItem(fadeOutSpec = null)
                )
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
        modifier = modifier
    ) {
        items(
            items = filters,
            key = { filter -> "${filter::class.simpleName}:${filter.id}" }
        ) { gameFilter ->
            CustomFilterChip(
                label = gameFilter.label.asString(),
                selected = true,
                onFilterClick = { onFilterClick(gameFilter) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                    )
                }
            )
        }
    }
}
