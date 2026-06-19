package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.VerticalGameCardSkeleton

@Composable
fun SearchSkeletonGrid(
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
        verticalItemSpacing = MaterialTheme.spacing.extraLarge,
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.large),
        modifier = modifier
    ) {
        items(6) {
            VerticalGameCardSkeleton()
        }
    }
}
