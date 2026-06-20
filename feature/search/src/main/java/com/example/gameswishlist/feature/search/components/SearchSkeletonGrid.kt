package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.VerticalGameCardSkeleton
import com.example.gameswishlist.core.ui.util.shimmerEffect

@Composable
fun SearchSkeletonGrid(
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
        verticalItemSpacing = MaterialTheme.spacing.extraLarge,
        contentPadding = PaddingValues(all = MaterialTheme.spacing.large),
        userScrollEnabled = false,
        modifier = modifier
    ) {
        items(6) {
            VerticalGameCardSkeleton()
        }
    }
}

@Composable
fun FilterChipsSkeletonRow(modifier: Modifier = Modifier) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
        userScrollEnabled = false,
        modifier = modifier
    ) {
        items(5) {
            FilterChipSkeleton()
        }
    }
}

@Composable
fun FilterChipSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(80.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(MaterialTheme.spacing.medium))
            .shimmerEffect()
    )
}
