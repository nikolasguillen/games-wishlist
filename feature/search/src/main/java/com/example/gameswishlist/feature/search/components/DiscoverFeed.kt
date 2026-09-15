package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 * The Discover feed: an editorial hero for the top anticipated pick, then the personalised shelves when
 * the user's library earned any, then "Most anticipated" (the rest of it) and "Popular this month" per
 * the design -- upcoming leads the generic pair since it is the one shelf a personalised feed would not
 * already cover with saved-game recommendations. [recommended] sits directly under the hero because
 * those are the only rows that are about this user; below the generic shelves they would read as an
 * afterthought. Falls back to the placeholder when every slot comes back empty rather than rendering
 * nothing. [hero] and [upcoming] are already disjoint -- [toDiscoverContentState][
 * com.example.gameswishlist.feature.search.mapper.toDiscoverContentState] does that split, this
 * composable only renders what it is given.
 *
 * [isStale] renders [DiscoverRefreshPrompt] as a [androidx.compose.foundation.lazy.LazyListScope.stickyHeader]
 * rather than a regular item, so it pins to the top of the list once the user scrolls past it instead of
 * scrolling away with the hero -- exactly the case a prompt that was just the first item would miss.
 */
@Composable
internal fun DiscoverFeed(
    hero: GameItemUiModel?,
    popular: List<GameItemUiModel>,
    upcoming: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    recommended: List<RecommendedShelfUiModel> = emptyList(),
    isStale: Boolean = false,
    isRefreshing: Boolean = false,
    onRefreshClick: () -> Unit = {},
    onDismissRefreshClick: () -> Unit = {},
    state: LazyListState = rememberLazyListState()
) {
    if (hero == null && popular.isEmpty() && upcoming.isEmpty() && recommended.isEmpty()) {
        DiscoverPlaceholder(modifier = modifier)
        return
    }

    LazyColumn(
        state = state,
        contentPadding = PaddingValues(vertical = MaterialTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
        modifier = modifier
    ) {
        // A sticky header, not an overlay: it takes up real space above the hero like any other item,
        // then pins to the top edge once scrolled past, instead of floating over whatever is
        // underneath it for the whole time it is visible.
        if (isStale) {
            stickyHeader {
                DiscoverRefreshPrompt(
                    isRefreshing = isRefreshing,
                    onClick = onRefreshClick,
                    onDismissClick = onDismissRefreshClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.large)
                )
            }
        }
        if (hero != null) {
            item {
                DiscoverHero(
                    game = hero,
                    onGameClick = onGameClick,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                )
            }
        }
        recommended.forEachIndexed { index, shelf ->
            item(key = "recommended_$index") {
                DiscoverShelf(
                    title = shelf.title.asString(),
                    games = shelf.games,
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

/**
 * A full-width bar, not a chip: [androidx.compose.material3.AssistChip] is fixed at Material's own 32.dp height, which leaves no
 * room to make the prompt read as more than a minor annotation, and a sticky header needs to span the
 * list's width anyway so nothing scrolling underneath shows through beside it once it pins. This is
 * hand-rolled so it can be sized and elevated to actually separate from the shelf scrolling under it.
 */
@Composable
private fun DiscoverRefreshPrompt(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = !isRefreshing,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = MaterialTheme.spacing.medium,
        shadowElevation = MaterialTheme.spacing.medium,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.large,
                end = MaterialTheme.spacing.small,
                top = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.medium
            )
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.discover_refresh_prompt_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.discover_refresh_prompt_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }

            IconButton(onClick = onDismissClick, enabled = !isRefreshing) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.discover_dismiss_refresh_prompt)
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
            recommended = listOf(
                RecommendedShelfUiModel(
                    title = UiText.StringResource(R.string.discover_more_from, "Larian Studios"),
                    games = previewGames
                ),
                RecommendedShelfUiModel(
                    title = UiText.StringResource(R.string.discover_because_you_like, "RPG"),
                    games = previewGames.reversed()
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverFeedStalePreview() {
    GamesWishlistTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewGames,
            upcoming = previewGames.drop(1),
            onGameClick = {},
            isStale = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DiscoverFeedRefreshingPreview() {
    GamesWishlistTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewGames,
            upcoming = previewGames.drop(1),
            onGameClick = {},
            isStale = true,
            isRefreshing = true
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
