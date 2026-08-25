package com.example.gameswishlist.feature.search.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.ui.component.EmptyPage
import com.example.gameswishlist.feature.search.R

@Composable
internal fun DiscoverPlaceholder(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.search_initial_message),
        icon = Icons.Outlined.SportsEsports,
        modifier = modifier
    )
}

@Composable
internal fun EmptySearchPlaceholder(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.search_no_results),
        icon = Icons.Outlined.SmartToy,
        modifier = modifier
    )
}

@Composable
internal fun NoFilteredResultsPlaceholder(modifier: Modifier = Modifier) {
    EmptyPage(
        message = stringResource(R.string.search_no_filtered_results),
        icon = Icons.Outlined.SearchOff,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun DiscoverPlaceholderPreview() {
    GamesWishlistTheme {
        DiscoverPlaceholder()
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptySearchPlaceholderPreview() {
    GamesWishlistTheme {
        EmptySearchPlaceholder()
    }
}

@Preview(showBackground = true)
@Composable
private fun NoFilteredResultsPlaceholderPreview() {
    GamesWishlistTheme {
        NoFilteredResultsPlaceholder()
    }
}
