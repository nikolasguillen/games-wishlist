package com.example.gameswishlist.feature.search.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.VerticalGameCard
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.feature.search.model.GameFilterUiModel

@Composable
fun SearchResultGrid(
    games: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
        verticalItemSpacing = MaterialTheme.spacing.extraLarge,
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.large),
        modifier = modifier
    ) {
        items(items = games, key = { it.id }) { game ->
            VerticalGameCard(game = game, onClick = { onGameClick(game.id) })
        }
    }
}

@Composable
fun FilterChipsRow(
    filters: List<GameFilterUiModel>,
    onFilterClick: (GameFilterUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
        modifier = modifier
    ) {
        items(
            items = filters,
            key = { filter -> "${filter::class.simpleName}:${filter.id}" }
        ) { gameFilter ->
            FilterChip(
                selected = gameFilter.selected,
                onClick = { onFilterClick(gameFilter) },
                label = {
                    Text(text = gameFilter.label.asString(), maxLines = 1)
                },
                leadingIcon = {
                    if (gameFilter.selected) {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                        )
                    }
                },
                contentPadding = PaddingValues(all = MaterialTheme.spacing.small),
                modifier = Modifier.animateContentSize()
            )
        }
    }
}
