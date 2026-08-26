package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.GameCompactCard
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.feature.search.R

/**
 * The cold-start Discover feed: an editorial hero for the top anticipated pick, then "Most
 * anticipated" (the rest of it) and "Popular this month" per the design -- upcoming leads since
 * it is the one shelf a personalised feed would not already cover with saved-game recommendations.
 * Falls back to the placeholder when both lists come back empty rather than rendering nothing.
 */
@Composable
internal fun DiscoverFeed(
    popular: List<GameItemUiModel>,
    upcoming: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState()
) {
    if (popular.isEmpty() && upcoming.isEmpty()) {
        DiscoverPlaceholder(modifier = modifier)
        return
    }

    // The top anticipated pick becomes the hero instead of also opening the shelf beneath it.
    val hero = upcoming.firstOrNull()
    val remainingUpcoming = upcoming.drop(1)

    LazyColumn(
        state = state,
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
        modifier = modifier
    ) {
        if (hero != null) {
            item {
                DiscoverHero(
                    game = hero,
                    onGameClick = onGameClick,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                )
            }
        }
        if (remainingUpcoming.isNotEmpty()) {
            item {
                DiscoverShelf(
                    title = stringResource(R.string.discover_most_anticipated),
                    games = remainingUpcoming,
                    onGameClick = onGameClick
                )
            }
        }
        if (popular.isNotEmpty()) {
            item {
                DiscoverShelf(
                    title = stringResource(R.string.discover_popular_this_month),
                    games = popular,
                    onGameClick = onGameClick
                )
            }
        }
    }
}

@Composable
private fun DiscoverShelf(
    title: String,
    games: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
        ) {
            items(items = games, key = { it.id }) { game ->
                GameCompactCard(
                    game = game,
                    onClick = { onGameClick(game.id) },
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

private val previewGames = listOf(
    GameItemUiModel.getDummy(),
    GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"),
    GameItemUiModel.getDummy().copy(id = 3, name = "Hollow Knight: Silksong")
)

@Preview(showBackground = true)
@Composable
private fun DiscoverFeedPreview() {
    GamesWishlistTheme {
        DiscoverFeed(
            popular = previewGames,
            upcoming = previewGames,
            onGameClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverFeedEmptyPreview() {
    GamesWishlistTheme {
        DiscoverFeed(
            popular = emptyList(),
            upcoming = emptyList(),
            onGameClick = {}
        )
    }
}
