package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.gamecard.RecentGameCard
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.feature.search.mapper.defaultBrowseGenres
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchHistoryUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchType
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.recently_viewed
import org.jetbrains.compose.resources.stringResource

/**
 * What the wide search panel shows before the first keystroke: the type chips, the genres to browse, the recent
 * searches with their "Clear all" button, and the recently viewed games. Everything wraps; nothing scrolls sideways.
 *
 * It draws its sections in the order of `searchDropdownItems` (the order the arrow keys walk), which
 * `SearchDropdownItemsTest` pins. Each actionable thing is handed to [onActivate] as the
 * [SearchDropdownItemUiModel] it is, and wears the modifier [itemModifier] builds for it, so the caller can mark and
 * track it without this composable knowing how.
 *
 * @param onShowRemovalDialog Asked to remove one recent search (a long press); the caller confirms with the user.
 * @param itemModifier Applied to a box around each actionable item.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WideSearchHome(
    selectedSearchType: SearchType,
    browseGenres: List<BrowseGenreUiModel>,
    history: SearchHistoryUiModel,
    onActivate: (SearchDropdownItemUiModel) -> Unit,
    onRemoveRecentGame: (Int) -> Unit,
    onShowRemovalDialog: (String) -> Unit,
    modifier: Modifier = Modifier,
    itemModifier: (SearchDropdownItemUiModel) -> Modifier = { Modifier }
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        modifier = modifier
    ) {
        SearchTypeChipRow(
            selected = selectedSearchType,
            onSelected = { onActivate(SearchDropdownItemUiModel.TypeChip(it)) },
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            chipModifier = { itemModifier(SearchDropdownItemUiModel.TypeChip(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        if (browseGenres.isNotEmpty()) {
            GenreBrowseSection(
                genres = browseGenres,
                onGenreClick = { onActivate(SearchDropdownItemUiModel.Genre(it.id)) },
                wrap = true,
                chipModifier = { itemModifier(SearchDropdownItemUiModel.Genre(it.id)) }
            )
        }

        if (history.isEmpty) {
            InitialMessage()
        }

        if (history.queries.isNotEmpty()) {
            RecentSearchesSection(
                recentSearches = history.queries,
                wrap = true,
                onClearRecentSearches = { onActivate(SearchDropdownItemUiModel.ClearHistory) },
                onHistoryItemClicked = { onActivate(SearchDropdownItemUiModel.RecentQuery(it)) },
                onShowRemovalDialog = onShowRemovalDialog,
                clearAllModifier = itemModifier(SearchDropdownItemUiModel.ClearHistory),
                chipModifier = { itemModifier(SearchDropdownItemUiModel.RecentQuery(it)) }
            )
        }

        if (history.games.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
                Text(
                    text = stringResource(Res.string.recently_viewed),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                ) {
                    history.games.forEach { game ->
                        Box(modifier = itemModifier(SearchDropdownItemUiModel.RecentGame(game.id))) {
                            // No modifier on the card: it only keeps its default size when none is given.
                            RecentGameCard(
                                game = game,
                                onClick = { onActivate(SearchDropdownItemUiModel.RecentGame(game.id)) },
                                onRemoveClick = { onRemoveRecentGame(game.id) }
                            )
                        }
                    }
                }
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

@Composable
private fun WideSearchHomePreview(
    browseGenres: List<BrowseGenreUiModel>,
    history: SearchHistoryUiModel
) {
    QuestLogTheme {
        Surface(color = MaterialTheme.appColors.expandedSearchBarColor) {
            WideSearchHome(
                selectedSearchType = SearchType.GAMES,
                browseGenres = browseGenres,
                history = history,
                onActivate = {},
                onRemoveRecentGame = {},
                onShowRemovalDialog = {}
            )
        }
    }
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchHomeFullPreview() {
    WideSearchHomePreview(browseGenres = defaultBrowseGenres(), history = previewHistory)
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchHomeNoHistoryPreview() {
    WideSearchHomePreview(browseGenres = defaultBrowseGenres(), history = SearchHistoryUiModel())
}

@QuestLogPreviews
@Preview(widthDp = 720)
@Composable
private fun WideSearchHomeFirstTimePreview() {
    WideSearchHomePreview(browseGenres = emptyList(), history = SearchHistoryUiModel())
}
