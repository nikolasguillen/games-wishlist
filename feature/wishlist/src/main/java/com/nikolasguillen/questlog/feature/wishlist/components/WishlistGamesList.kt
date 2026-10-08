package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFabDefaults
import com.nikolasguillen.questlog.core.ui.component.gamecard.VerticalGameCard
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.wishlist.R
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistSectionUiModel

/** Spans the whole line of the two-column grid: the header, the options row, section headers and list rows. */
private val FullLineSpan: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

/**
 * Both layouts live in one lazy grid so the header, the options row and the status headers are written once
 * and a single scroll state serves either view. In [WishlistViewMode.LIST] every game takes a full line (a
 * swipeable row); in [WishlistViewMode.GRID] every game is one cell, two to a line. A full-line item always
 * starts a new line, so a status header never shares a line with the last card of the section above it.
 *
 * Spacing between grid lines comes from padding on each card rather than from `verticalArrangement`, which
 * would also open gaps between the list view's rows.
 */
// sections is always the same instance from WishlistContentState.Success until it actually changes,
// so the instability falls back to reference comparison, which already skips correctly.
@Composable
internal fun WishlistGamesList(
    sections: List<WishlistSectionUiModel>,
    viewMode: WishlistViewMode,
    gridState: LazyGridState,
    revealedGameId: Int?,
    onRevealedGameIdChange: (Int?) -> Unit,
    onGameClick: (Int) -> Unit,
    onGameRemoveClick: (GameItemUiModel) -> Unit,
    header: @Composable () -> Unit,
    viewOptionsRow: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // A long press on a grid card removes the game, so that is what accessibility services announce.
    val removeGameLabel = stringResource(R.string.remove_game_action)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        // Room under the last line for the screen's floating scroll-to-top button, so the last game is never
        // left covered.
        contentPadding = PaddingValues(bottom = ScrollToTopFabDefaults.ContentBottomPadding),
        modifier = modifier.fillMaxSize()
    ) {
        item(key = "list_header", span = FullLineSpan, contentType = "list_header") { header() }
        item(key = "view_options", span = FullLineSpan, contentType = "view_options") { viewOptionsRow() }
        sections.forEach { section ->
            item(key = "header_${section.status}", span = FullLineSpan, contentType = "header") {
                StatusSectionHeader(
                    label = section.label.asString(),
                    count = section.games.size,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                )
            }
            when (viewMode) {
                WishlistViewMode.LIST -> section.games.forEach { game ->
                    item(
                        key = "game_${section.status}_${game.id}",
                        span = FullLineSpan,
                        contentType = "game_row"
                    ) {
                        SwipeToRevealRow(
                            isRevealed = revealedGameId == game.id,
                            onRevealChange = { revealed ->
                                if (revealed) {
                                    onRevealedGameIdChange(game.id)
                                } else if (revealedGameId == game.id) {
                                    // Ignore the closing of a row that is no longer the open one: when A
                                    // closes because B just opened, A notifies "closed" after B has
                                    // already registered, which would reset B.
                                    onRevealedGameIdChange(null)
                                }
                            },
                            onRemoveClick = { onGameRemoveClick(game) }
                        ) {
                            WishlistGameRow(
                                game = game,
                                onClick = {
                                    if (revealedGameId == game.id) {
                                        onRevealedGameIdChange(null)
                                    } else {
                                        onGameClick(game.id)
                                    }
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }
                }

                WishlistViewMode.GRID -> itemsIndexed(
                    items = section.games,
                    key = { _, game -> "game_${section.status}_${game.id}" },
                    contentType = { _, _ -> "game_card" }
                ) { index, game ->
                    // The index is within the section: every section starts on a fresh line, so parity
                    // alone says which column the card is in.
                    val isLeftColumn = index % 2 == 0
                    val isRightColumn = index % 2 == 1

                    // No save button: the heart would show membership of the default list, not of this one.
                    VerticalGameCard(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        onLongClickLabel = removeGameLabel,
                        onLongClick = { onGameRemoveClick(game) },
                        modifier = Modifier
                            .animateItem(fadeOutSpec = null)
                            .padding(
                                start = if (isLeftColumn) MaterialTheme.spacing.large else MaterialTheme.spacing.default,
                                end = if (isRightColumn) MaterialTheme.spacing.large else MaterialTheme.spacing.default,
                                bottom = MaterialTheme.spacing.large
                            )
                    )
                }
            }
            item(key = "spacer_${section.status}", span = FullLineSpan, contentType = "spacer") {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            }
        }
    }
}

private val previewSections = listOf(
    WishlistSectionUiModel(
        status = GameStatus.PLAYING,
        label = UiText.DynamicString("PLAYING"),
        games = listOf(GameItemUiModel.getDummy())
    ),
    WishlistSectionUiModel(
        status = null,
        label = UiText.DynamicString("NO STATUS"),
        games = listOf(GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"))
    )
)

/** An odd-sized section followed by a one-game section: the lone card and the header on a fresh line. */
private val previewOddSections = listOf(
    WishlistSectionUiModel(
        status = GameStatus.PLAYING,
        label = UiText.DynamicString("PLAYING"),
        games = listOf(
            GameItemUiModel.getDummy(),
            GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"),
            GameItemUiModel.getDummy().copy(id = 3, name = "Hades")
        )
    ),
    WishlistSectionUiModel(
        status = GameStatus.COMPLETED,
        label = UiText.DynamicString("COMPLETED"),
        games = listOf(GameItemUiModel.getDummy().copy(id = 4, name = "Celeste"))
    )
)

@Composable
private fun WishlistGamesListPreview(
    viewMode: WishlistViewMode,
    sections: List<WishlistSectionUiModel> = previewSections
) {
    QuestLogTheme {
        WishlistGamesList(
            sections = sections,
            viewMode = viewMode,
            gridState = rememberLazyGridState(),
            revealedGameId = null,
            onRevealedGameIdChange = {},
            onGameClick = {},
            onGameRemoveClick = {},
            header = {
                WishlistDetailHeader(
                    title = "Couch Co-op",
                    description = "Games worth playing together.",
                    iconRes = WishlistIcon.MULTIPLAYER.toDrawableRes(),
                    coverImagePath = null,
                    gameCountText = "2 games",
                    isDefaultList = false
                )
            },
            viewOptionsRow = {
                WishlistViewOptionsRow(
                    viewMode = viewMode,
                    chips = emptyList(),
                    onChipClick = {},
                    onToggleClick = {}
                )
            }
        )
    }
}

@QuestLogPreviews
@Composable
private fun WishlistGamesListPreview() {
    WishlistGamesListPreview(viewMode = WishlistViewMode.LIST)
}

@QuestLogPreviews
@Composable
private fun WishlistGamesGridPreview() {
    WishlistGamesListPreview(viewMode = WishlistViewMode.GRID)
}

@QuestLogPreviews
@Composable
private fun WishlistGamesGridOddCountsPreview() {
    WishlistGamesListPreview(viewMode = WishlistViewMode.GRID, sections = previewOddSections)
}
