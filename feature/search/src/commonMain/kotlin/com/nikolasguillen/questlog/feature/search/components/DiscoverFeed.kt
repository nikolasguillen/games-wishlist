package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFabDefaults
import com.nikolasguillen.questlog.core.ui.component.gamecard.CompactGameCard
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.mapper.DISCOVER_SHELF_ROW_SIZE
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfKey
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_because_you_like
import com.nikolasguillen.questlog.feature.search.resources.discover_more_from
import com.nikolasguillen.questlog.feature.search.resources.discover_most_anticipated
import com.nikolasguillen.questlog.feature.search.resources.discover_popular_this_month
import com.nikolasguillen.questlog.feature.search.resources.discover_see_all_shelf
import org.jetbrains.compose.resources.stringResource

/**
 * The Discover feed: an editorial hero for the top anticipated pick, then the personalised shelves when
 * the user's library earned any, then "Most anticipated" (the rest of it) and "Popular this month" per
 * the design -- upcoming leads the generic pair since it is the one shelf a personalised feed would not
 * already cover with saved-game recommendations. [recommended] sits directly under the hero because
 * those are the only rows that are about this user; below the generic shelves they would read as an
 * afterthought. Falls back to the placeholder when every slot comes back empty rather than rendering
 * nothing. [hero] and [upcoming] are already disjoint -- [toDiscoverContentState][
 * com.nikolasguillen.questlog.feature.search.mapper.toDiscoverContentState] does that split, this
 * composable only renders what it is given.
 *
 * [isStale] draws [DiscoverRefreshPrompt] in an [AnimatedVisibility] overlay at the top of the feed rather
 * than as a list item, so it stays in view once the user scrolls past the hero instead of scrolling away
 * with it -- exactly the case a prompt that was just the first item would miss.
 */
@Composable
internal fun DiscoverFeed(
    hero: GameItemUiModel?,
    popular: DiscoverShelfUiModel?,
    upcoming: DiscoverShelfUiModel?,
    onGameClick: (Int) -> Unit,
    onSaveClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier,
    recommended: List<DiscoverShelfUiModel> = emptyList(),
    isStale: Boolean = false,
    isRefreshing: Boolean = false,
    onRefreshClick: () -> Unit = {},
    onDismissRefreshClick: () -> Unit = {},
    state: LazyListState = rememberLazyListState()
) {
    if (hero == null && popular == null && upcoming == null && recommended.isEmpty()) {
        DiscoverPlaceholder(modifier = modifier)
        return
    }

    Box {
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(
                top = MaterialTheme.spacing.large,
                bottom = ScrollToTopFabDefaults.ContentBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraLarge),
            modifier = modifier
        ) {
            if (hero != null) {
                item(key = DISCOVER_HERO_KEY) {
                    DiscoverHero(
                        game = hero,
                        onGameClick = onGameClick,
                        onSaveClick = { onSaveClick(hero.id) },
                        onLongClick = { onLongClick(hero.id) },
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                    )
                }
            }
            recommended.forEachIndexed { index, shelf ->
                item(key = discoverRecommendedKey(index)) {
                    DiscoverShelf(
                        shelf = shelf,
                        onGameClick = onGameClick,
                        onSaveClick = onSaveClick,
                        onLongClick = onLongClick,
                        onSeeAllClick = onSeeAllClick
                    )
                }
            }
            if (upcoming != null) {
                item(key = DISCOVER_UPCOMING_KEY) {
                    DiscoverShelf(
                        shelf = upcoming,
                        onGameClick = onGameClick,
                        onSaveClick = onSaveClick,
                        onLongClick = onLongClick,
                        onSeeAllClick = onSeeAllClick
                    )
                }
            }
            if (popular != null) {
                item(key = DISCOVER_POPULAR_KEY) {
                    DiscoverShelf(
                        shelf = popular,
                        onGameClick = onGameClick,
                        onSaveClick = onSaveClick,
                        onLongClick = onLongClick,
                        onSeeAllClick = onSeeAllClick
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isStale,
            enter = fadeIn() + slideInVertically { -it },
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            DiscoverRefreshPrompt(
                isRefreshing = isRefreshing,
                onClick = onRefreshClick,
                onDismissClick = onDismissRefreshClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
                    .padding(top = MaterialTheme.spacing.extraLarge)
            )
        }
    }

}

/**
 * One shelf: a title row, then the capped row of cards. Only a shelf that [DiscoverShelfUiModel.hasMore] gets
 * an arrow, and then the whole title row is the button -- announced once, as "See all: <title>", rather
 * than as a title and an arrow that do the same thing. Without an arrow the row is a plain heading and does
 * nothing, since a grid of the same cards would add nothing.
 */
@Composable
private fun DiscoverShelf(
    shelf: DiscoverShelfUiModel,
    onGameClick: (Int) -> Unit,
    onSaveClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = modifier
    ) {
        DiscoverShelfTitleRow(shelf = shelf, onSeeAllClick = onSeeAllClick)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
        ) {
            items(items = shelf.games, key = { it.id }) { game ->
                CompactGameCard(
                    game = game,
                    onClick = { onGameClick(game.id) },
                    onSaveClick = { onSaveClick(game.id) },
                    onSaveLongClick = { onLongClick(game.id) },
                    modifier = Modifier.width(140.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscoverShelfTitleRow(
    shelf: DiscoverShelfUiModel,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val seeAllLabel = if (shelf.hasMore) {
        stringResource(Res.string.discover_see_all_shelf, shelf.title.asString())
    } else {
        null
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (seeAllLabel != null) {
                    // One merged node: the title and the arrow are one control, so a screen reader stops
                    // here once. clearAndSetSemantics comes after clickable, which keeps the button role.
                    Modifier
                        .clickable(role = Role.Button) { onSeeAllClick(shelf) }
                        .clearAndSetSemantics { contentDescription = seeAllLabel }
                } else {
                    Modifier
                }
            )
            // Every heading takes the platform's minimum touch height, with or without an arrow, so the
            // shelves keep one rhythm; only the ones with an arrow are tappable.
            .minimumInteractiveComponentSize()
            .padding(horizontal = MaterialTheme.spacing.large)
    ) {
        Text(
            text = shelf.title.asString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (shelf.hasMore) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.spacing.large)
            )
        }
    }
}

private val previewGames = listOf(
    GameItemUiModel.getDummy(),
    GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"),
    GameItemUiModel.getDummy().copy(id = 3, name = "Hollow Knight: Silksong")
)

private fun previewShelf(
    key: DiscoverShelfKey,
    title: UiText,
    games: List<GameItemUiModel> = previewGames,
    hasMore: Boolean = false
) = DiscoverShelfUiModel(key = key, title = title, games = games, hasMore = hasMore)

private val previewPopular = previewShelf(
    key = DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH),
    title = UiText.StringResource(Res.string.discover_popular_this_month)
)

private val previewUpcoming = previewShelf(
    key = DiscoverShelfKey.Lane(DiscoverLane.MOST_ANTICIPATED),
    title = UiText.StringResource(Res.string.discover_most_anticipated),
    games = previewGames.drop(1)
)

/** Exactly as many cards as a row holds: a full row, and still no arrow. */
private val previewFullRow = previewShelf(
    key = DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH),
    title = UiText.StringResource(Res.string.discover_popular_this_month),
    games = (1..DISCOVER_SHELF_ROW_SIZE).map {
        GameItemUiModel.getDummy().copy(id = it, name = "Game $it")
    }
)

@QuestLogPreviews
@Composable
private fun DiscoverFeedPreview() {
    QuestLogTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewPopular,
            upcoming = previewUpcoming,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverFeedFullRowPreview() {
    QuestLogTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewFullRow,
            upcoming = null,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverFeedSeeAllPreview() {
    QuestLogTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewPopular.copy(hasMore = true),
            upcoming = previewUpcoming,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {},
            recommended = listOf(
                previewShelf(
                    key = DiscoverShelfKey.ByDeveloper(developerId = 1),
                    title = UiText.StringResource(
                        Res.string.discover_more_from,
                        "A studio with a name far too long to fit beside the arrow on a narrow screen"
                    ),
                    hasMore = true
                )
            )
        )
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverFeedRecommendedPreview() {
    QuestLogTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewPopular,
            upcoming = previewUpcoming,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {},
            recommended = listOf(
                previewShelf(
                    key = DiscoverShelfKey.ByDeveloper(developerId = 1),
                    title = UiText.StringResource(Res.string.discover_more_from, "Larian Studios")
                ),
                previewShelf(
                    key = DiscoverShelfKey.ByGenre(genreId = 12),
                    title = UiText.StringResource(Res.string.discover_because_you_like, "RPG"),
                    games = previewGames.reversed()
                )
            )
        )
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverFeedStalePreview() {
    QuestLogTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewPopular,
            upcoming = previewUpcoming,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {},
            isStale = true
        )
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverFeedRefreshingPreview() {
    QuestLogTheme {
        DiscoverFeed(
            hero = previewGames.first(),
            popular = previewPopular,
            upcoming = previewUpcoming,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {},
            isStale = true,
            isRefreshing = true
        )
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverFeedEmptyPreview() {
    QuestLogTheme {
        DiscoverFeed(
            hero = null,
            popular = null,
            upcoming = null,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {}
        )
    }
}
