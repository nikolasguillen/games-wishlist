package com.example.gameswishlist.feature.radar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.ui.component.EmptyPage
import com.example.gameswishlist.feature.radar.R

@Composable
internal fun RadarEmptyState(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.radar_empty_title),
        subtitle = stringResource(R.string.radar_empty_subtitle),
        icon = Icons.Default.Radar,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun RadarEmptyStatePreview() {
    GamesWishlistTheme {
        RadarEmptyState()
    }
}
