package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomChipRow
import com.nikolasguillen.questlog.core.ui.component.CustomTextButton
import com.nikolasguillen.questlog.core.ui.component.gamecard.RecentGameCard
import com.nikolasguillen.questlog.core.ui.component.gamecard.RecentGameCardDefaults
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.util.isLandscapeWindow
import com.nikolasguillen.questlog.feature.search.mapper.defaultBrowseGenres
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchHistoryUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.clear_all
import com.nikolasguillen.questlog.feature.search.resources.expanded_search_initial_message
import com.nikolasguillen.questlog.feature.search.resources.recent_searches
import com.nikolasguillen.questlog.feature.search.resources.recently_viewed
import org.jetbrains.compose.resources.stringResource

/** The width a recent game's card gets in the landscape grid, and so how many fit in a row. */
private val RECENT_GAME_MIN_WIDTH = RecentGameCardDefaults.Width

/** A recent game's cover is portrait art, so its card is taller than wide. */
private const val RECENT_GAME_ASPECT_RATIO = 3f / 4f

/**
 * What the expanded search overlay shows before the first keystroke: genres to browse, the recent searches and the
 * recently viewed games.
 *
 * In portrait these stack in one scrolling column. In landscape the overlay is short and wide, so it splits in two:
 * genres and recent searches (wrapping, not scrolling sideways) on one side and the recent games, in a grid with
 * taller cards, on the other. A side with nothing to show gives its width to the other.
 *
 * @param onClearRecentSearches Asked to clear the history; the caller confirms with the user first.
 * @param onShowRemovalDialog Asked to remove one recent search; the caller confirms with the user first.
 */
@Composable
internal fun ExpandedSearchHome(
    history: SearchHistoryUiModel,
    browseGenres: List<BrowseGenreUiModel>,
    onBrowseGenre: (BrowseGenreUiModel) -> Unit,
    onCommitSearch: (String) -> Unit,
    onGameClick: (Int) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    onClearRecentSearches: () -> Unit,
    onShowRemovalDialog: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLandscapeWindow()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                .padding(vertical = MaterialTheme.spacing.medium)
        ) {
            if (browseGenres.isNotEmpty() || history.queries.isNotEmpty() || history.isEmpty) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (browseGenres.isNotEmpty()) {
                        GenreBrowseSection(
                            genres = browseGenres,
                            onGenreClick = onBrowseGenre
                        )
                    }
                    if (history.isEmpty) {
                        InitialMessage()
                    }
                    if (history.queries.isNotEmpty()) {
                        RecentSearchesSection(
                            recentSearches = history.queries,
                            wrap = true,
                            onClearRecentSearches = onClearRecentSearches,
                            onHistoryItemClicked = onCommitSearch,
                            onShowRemovalDialog = onShowRemovalDialog
                        )
                    }
                }
            }
            if (history.games.isNotEmpty()) {
                RecentGamesGrid(
                    recentGames = history.games,
                    onGameClick = onGameClick,
                    onRemoveClick = onRemoveRecentGame,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = MaterialTheme.spacing.medium)
        ) {
            // Above the history, and also when there is none: a first-time user has nothing to
            // revisit, which is exactly when a way into the catalogue is most useful.
            if (browseGenres.isNotEmpty()) {
                GenreBrowseSection(
                    genres = browseGenres,
                    onGenreClick = onBrowseGenre
                )
            }
            if (history.isEmpty) {
                InitialMessage()
            }
            if (history.queries.isNotEmpty()) {
                RecentSearchesSection(
                    recentSearches = history.queries,
                    wrap = false,
                    onClearRecentSearches = onClearRecentSearches,
                    onHistoryItemClicked = onCommitSearch,
                    onShowRemovalDialog = onShowRemovalDialog
                )
            }
            if (history.games.isNotEmpty()) {
                RecentGamesRow(
                    recentGames = history.games,
                    onGameClick = onGameClick,
                    onRemoveClick = onRemoveRecentGame
                )
            }
        }
    }
}

@Composable
internal fun InitialMessage() {
    Text(
        text = stringResource(Res.string.expanded_search_initial_message),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(MaterialTheme.spacing.large)
            .fillMaxWidth()
    )
}

/**
 * The recent searches, under a heading with a "clear all" button.
 *
 * @param wrap Lay the chips out in a wrapping grid instead of one row that scrolls sideways.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    wrap: Boolean,
    onClearRecentSearches: () -> Unit,
    onHistoryItemClicked: (String) -> Unit,
    onShowRemovalDialog: (String) -> Unit
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.large)
        ) {
            Text(
                text = stringResource(Res.string.recent_searches),
                style = MaterialTheme.typography.titleMedium
            )

            CustomTextButton(
                onClick = onClearRecentSearches,
            ) {
                Text(stringResource(Res.string.clear_all))
            }
        }
        // A tap runs the search and a long press asks to remove it: neither leaves a chip to bring into view.
        if (wrap) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
            ) {
                recentSearches.forEach { recentSearch ->
                    RecentSearchChip(
                        recentSearch = recentSearch,
                        onClick = { onHistoryItemClicked(recentSearch) },
                        onLongClick = { onShowRemovalDialog(recentSearch) }
                    )
                }
            }
        } else {
            CustomChipRow(
                items = recentSearches,
                key = { it.hashCode() }
            ) { recentSearch, _ ->
                RecentSearchChip(
                    recentSearch = recentSearch,
                    onClick = { onHistoryItemClicked(recentSearch) },
                    onLongClick = { onShowRemovalDialog(recentSearch) }
                )
            }
        }
    }
}

@Composable
private fun RecentSearchChip(
    recentSearch: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val inputChipInteractionSource = remember { MutableInteractionSource() }
    Box {
        SuggestionChip(
            onClick = onClick,
            label = {
                Text(
                    text = recentSearch,
                    maxLines = 1
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null
                )
            },
            contentPadding = PaddingValues(all = MaterialTheme.spacing.small),
            interactionSource = inputChipInteractionSource,
            colors = SuggestionChipDefaults.suggestionChipColors(
                iconContentColor = MaterialTheme.colorScheme.primary
            )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = onClick,
                    interactionSource = inputChipInteractionSource,
                    indication = null
                )
        )
    }
}

@Composable
private fun RecentGamesRow(
    recentGames: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
        Text(
            text = stringResource(Res.string.recently_viewed),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
        ) {
            items(items = recentGames, key = { it.id }) { game ->
                RecentGameCard(
                    game = game,
                    onClick = { onGameClick(game.id) },
                    onRemoveClick = { onRemoveClick(game.id) }
                )
            }
        }
    }
}

/** The recent games as a grid that scrolls on its own, each card as wide as its cell and taller than wide. */
@Composable
private fun RecentGamesGrid(
    recentGames: List<GameItemUiModel>,
    onGameClick: (Int) -> Unit,
    onRemoveClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = modifier
    ) {
        Text(
            text = stringResource(Res.string.recently_viewed),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(RECENT_GAME_MIN_WIDTH),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.large,
                end = MaterialTheme.spacing.large,
                bottom = MaterialTheme.spacing.large
            ),
            modifier = Modifier.weight(1f)
        ) {
            items(items = recentGames, key = { it.id }) { game ->
                RecentGameCard(
                    game = game,
                    onClick = { onGameClick(game.id) },
                    onRemoveClick = { onRemoveClick(game.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(RECENT_GAME_ASPECT_RATIO)
                )
            }
        }
    }
}

private val previewHistory = SearchHistoryUiModel(
    queries = listOf("The Witcher", "Cyberpunk 2077", "Hollow Knight", "Elden Ring", "Baldur's Gate 3", "Zelda"),
    games = List(8) { index ->
        GameItemUiModel.getDummy().copy(id = index + 1, name = "Game ${index + 1}")
    }
)

@QuestLogPreviews
@Composable
private fun ExpandedSearchHomePreview() {
    QuestLogTheme {
        ExpandedSearchHome(
            history = previewHistory,
            browseGenres = defaultBrowseGenres(),
            onBrowseGenre = {},
            onCommitSearch = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onShowRemovalDialog = {}
        )
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Composable
private fun ExpandedSearchHomeLandscapePreview() {
    QuestLogTheme {
        ExpandedSearchHome(
            history = previewHistory,
            browseGenres = defaultBrowseGenres(),
            onBrowseGenre = {},
            onCommitSearch = {},
            onGameClick = {},
            onRemoveRecentGame = {},
            onClearRecentSearches = {},
            onShowRemovalDialog = {}
        )
    }
}
