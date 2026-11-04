package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.DiscoverLane
import com.nikolasguillen.questlog.core.ui.component.CustomTextButton
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFabDefaults
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfKey
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_because_you_like
import com.nikolasguillen.questlog.feature.search.resources.discover_more_from
import com.nikolasguillen.questlog.feature.search.resources.discover_most_anticipated
import com.nikolasguillen.questlog.feature.search.resources.discover_popular_this_month
import com.nikolasguillen.questlog.feature.search.resources.discover_release_date_disclaimer
import com.nikolasguillen.questlog.feature.search.resources.discover_see_all
import com.nikolasguillen.questlog.feature.search.resources.discover_see_all_shelf
import org.jetbrains.compose.resources.stringResource

/**
 * The Discover feed for a window at least 840dp wide: the same content as [DiscoverFeed] in a layout of its own,
 * a tall hero banner with its own actions, then one full-width row of cards per shelf.
 *
 * It emits the same items, in the same order and under the same keys as [DiscoverFeed] (see
 * `DiscoverFeedItemKeys.kt`) and takes the same [state], so a resize across the wide-window threshold swaps the
 * layout without a reload and keeps the first visible shelf. With nothing to show it falls back to the placeholder,
 * as [DiscoverFeed] does. [isStale] draws [DiscoverRefreshPrompt] in the same top overlay.
 */
@Composable
internal fun WideDiscoverFeed(
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
                    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)) {
                        WideDiscoverHero(
                            game = hero,
                            onDetailsClick = { onGameClick(hero.id) },
                            onSaveClick = { onSaveClick(hero.id) },
                            onSaveLongClick = { onLongClick(hero.id) }
                        )
                        Text(
                            text = stringResource(Res.string.discover_release_date_disclaimer),
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(
                                top = MaterialTheme.spacing.small,
                                start = MaterialTheme.spacing.small
                            )
                        )
                    }
                }
            }
            recommended.forEachIndexed { index, shelf ->
                item(key = discoverRecommendedKey(index)) {
                    WideDiscoverShelf(
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
                    WideDiscoverShelf(
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
                    WideDiscoverShelf(
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
 * One shelf: a title row, then a single row of cards that fills the width.
 *
 * The row holds as many cards as its own width allows ([wideShelfColumnCount]), up to the games the shelf carries,
 * in equal-width slots, so a card is never cut off and never stretched: a shelf with fewer games than slots leaves
 * the rest empty. It is a plain [Row], never a horizontal scroll, because a mouse wheel does not scroll sideways.
 * The "See all" link shows when the shelf has more games than it carries or when this width hides some of the ones
 * it does ([wideShelfShowsSeeAll]).
 */
@Composable
private fun WideDiscoverShelf(
    shelf: DiscoverShelfUiModel,
    onGameClick: (Int) -> Unit,
    onSaveClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = wideShelfColumnCount(
            availableWidth = maxWidth,
            spacing = MaterialTheme.spacing.large,
            edge = MaterialTheme.spacing.large
        )

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            WideDiscoverShelfTitleRow(
                shelf = shelf,
                showsSeeAll = wideShelfShowsSeeAll(
                    hasMore = shelf.hasMore,
                    gameCount = shelf.games.size,
                    columns = columns
                ),
                onSeeAllClick = onSeeAllClick
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
            ) {
                repeat(columns) { slot ->
                    val game = shelf.games.getOrNull(slot)
                    key(game?.id ?: -(slot + 1)) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (game != null) {
                                WideDiscoverShelfCard(
                                    game = game,
                                    onClick = { onGameClick(game.id) },
                                    onSaveClick = { onSaveClick(game.id) },
                                    onSaveLongClick = { onLongClick(game.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The shelf's heading and, when [showsSeeAll], its "See all" link, announced as "See all: <title>" so a screen
 * reader hears which shelf it opens. Every shelf's row keeps the minimum interactive height, with or without the
 * link, so the shelves keep one rhythm.
 */
@Composable
private fun WideDiscoverShelfTitleRow(
    shelf: DiscoverShelfUiModel,
    showsSeeAll: Boolean,
    onSeeAllClick: (DiscoverShelfUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = shelf.title.asString()
    val seeAllLabel = stringResource(Res.string.discover_see_all_shelf, title)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .padding(horizontal = MaterialTheme.spacing.large)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )

        if (showsSeeAll) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            CustomTextButton(
                onClick = { onSeeAllClick(shelf) },
                modifier = Modifier.semantics { contentDescription = seeAllLabel }
            ) {
                Text(text = stringResource(Res.string.discover_see_all))
            }
        }
    }
}

private fun previewGames(count: Int) = (1..count).map {
    GameItemUiModel.getDummy().copy(id = it, name = "Game $it", isSaved = it % 3 == 0)
}

private fun previewShelf(
    key: DiscoverShelfKey,
    title: UiText,
    gameCount: Int = 6,
    hasMore: Boolean = false
) = DiscoverShelfUiModel(key = key, title = title, games = previewGames(gameCount), hasMore = hasMore)

private val previewHero = GameItemUiModel.getDummy().copy(name = "Hollow Knight: Silksong")

private val previewPopular = previewShelf(
    key = DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH),
    title = UiText.StringResource(Res.string.discover_popular_this_month)
)

private val previewUpcoming = previewShelf(
    key = DiscoverShelfKey.Lane(DiscoverLane.MOST_ANTICIPATED),
    title = UiText.StringResource(Res.string.discover_most_anticipated),
    hasMore = true
)

@Composable
private fun PreviewFeed(
    popular: DiscoverShelfUiModel? = previewPopular,
    upcoming: DiscoverShelfUiModel? = previewUpcoming,
    recommended: List<DiscoverShelfUiModel> = emptyList(),
    isStale: Boolean = false
) {
    QuestLogTheme {
        WideDiscoverFeed(
            hero = previewHero,
            popular = popular,
            upcoming = upcoming,
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onSeeAllClick = {},
            recommended = recommended,
            isStale = isStale
        )
    }
}

@QuestLogPreviews
@Composable
private fun WideDiscoverFeedPreview() {
    PreviewFeed()
}

/** Fewer games than the row has slots: the rest of the row stays empty and no card stretches. */
@QuestLogPreviews
@Composable
private fun WideDiscoverFeedFewGamesPreview() {
    PreviewFeed(
        popular = previewShelf(
            key = DiscoverShelfKey.Lane(DiscoverLane.POPULAR_THIS_MONTH),
            title = UiText.StringResource(Res.string.discover_popular_this_month),
            gameCount = 3
        ),
        upcoming = null
    )
}

@QuestLogPreviews
@Composable
private fun WideDiscoverFeedLongTitlePreview() {
    PreviewFeed(
        recommended = listOf(
            previewShelf(
                key = DiscoverShelfKey.ByDeveloper(developerId = 1),
                title = UiText.StringResource(
                    Res.string.discover_more_from,
                    "A studio with a name far too long to fit beside the link on a narrow window"
                ),
                hasMore = true
            )
        )
    )
}

@QuestLogPreviews
@Composable
private fun WideDiscoverFeedStalePreview() {
    PreviewFeed(isStale = true)
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun WideDiscoverFeedDesktopPreview() {
    PreviewFeed(
        recommended = listOf(
            previewShelf(
                key = DiscoverShelfKey.ByGenre(genreId = 12),
                title = UiText.StringResource(Res.string.discover_because_you_like, "RPG")
            )
        )
    )
}

@Preview(widthDp = 560, heightDp = 800)
@Composable
private fun WideDiscoverFeedNarrowPreview() {
    PreviewFeed()
}
