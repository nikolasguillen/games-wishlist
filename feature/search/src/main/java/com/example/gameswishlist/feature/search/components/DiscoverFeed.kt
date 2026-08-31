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
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.feature.search.model.RecommendedShelfUiModel

/**
 * The Discover feed: an editorial hero for the top anticipated pick, then the personalised shelf when
 * the user's library earned one, then "Most anticipated" (the rest of it) and "Popular this month" per
 * the design -- upcoming leads the generic pair since it is the one shelf a personalised feed would not
 * already cover with saved-game recommendations. [recommended] sits directly under the hero because it
 * is the only row that is about this user; below the generic shelves it would read as an afterthought.
 * Falls back to the placeholder when every slot comes back empty rather than rendering nothing.
 * [hero] and [upcoming] are already disjoint -- [toDiscoverContentState][
 * com.example.gameswishlist.feature.search.mapper.toDiscoverContentState] does that split, this
 * composable only renders what it is given.
 */
@Composable
internal fun DiscoverFeed(
    hero: GameItemUiModel?,
    popular: List<GameItemUiModel>,
    upcoming: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    recommended: RecommendedShelfUiModel? = null,
    state: LazyListState = rememberLazyListState()
) {
    if (hero == null && popular.isEmpty() && upcoming.isEmpty() && recommended == null) {
        DiscoverPlaceholder(modifier = modifier)
        return
    }

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
        if (recommended != null) {
            item {
                DiscoverShelf(
                    title = recommended.title.asString(),
                    games = recommended.games,
                    onGameClick = onGameClick
                )
            }
        }
        if (upcoming.isNotEmpty()) {
            item {
                DiscoverShelf(
                    title = stringResource(R.string.discover_most_anticipated),
                    games = upcoming,
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
            hero = previewGames.first(),
            popular = previewGames,
            upcoming = previewGames.drop(1),
            onGameClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverFeedRecommendedPreview() {
    GamesWishlistTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewGames,
            upcoming = previewGames.drop(1),
            onGameClick = {},
            recommended = RecommendedShelfUiModel(
                title = UiText.StringResource(R.string.discover_because_you_like, "RPG"),
                games = previewGames
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverFeedEmptyPreview() {
    GamesWishlistTheme {
        DiscoverFeed(
            hero = null,
            popular = emptyList(),
            upcoming = emptyList(),
            onGameClick = {}
        )
    }
}
