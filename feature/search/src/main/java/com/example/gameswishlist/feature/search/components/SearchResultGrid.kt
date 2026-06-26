package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    state: LazyGridState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (games.isNotEmpty()) {
            state.scrollToItem(0)
        }
    }

    val filtersHeader = @Composable {
        if (activeFilters.isNotEmpty()) {
            ActiveFiltersRow(
                filters = activeFilters,
                onFilterClick = onFilterClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (games.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            filtersHeader()
            NoFilteredResultsPlaceholder(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.spacing.large)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            contentPadding = PaddingValues(bottom = MaterialTheme.spacing.medium),
            modifier = modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                filtersHeader()
            }

            itemsIndexed(
                items = games,
                key = { _, game -> game.id }
            ) { index, game ->
                val isLeftColumn = index % 2 == 0
                val isRightColumn = index % 2 == 1

                VerticalGameCard(
                    game = game,
                    onClick = { onGameClick(game.id) },
                    modifier = Modifier
                        .animateItem(fadeOutSpec = null)
                        .padding(
                            start = if (isLeftColumn) MaterialTheme.spacing.large else 0.dp,
                            end = if (isRightColumn) MaterialTheme.spacing.large else 0.dp
                        )
                )
            }
        }
    }
}

@Composable
private fun ActiveFiltersRow(
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
