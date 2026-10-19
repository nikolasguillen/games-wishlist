package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomChipRow
import com.nikolasguillen.questlog.core.ui.component.CustomFilterChip
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFabDefaults
import com.nikolasguillen.questlog.core.ui.component.gamecard.VerticalGameCard
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.choose_list_content_description
import com.nikolasguillen.questlog.core.ui.util.isLandscapeShape
import com.nikolasguillen.questlog.feature.search.model.GameFilterUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.remove_filter_content_description
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun SearchResultGrid(
    games: List<GameItemUiModel>,
    activeFilters: List<GameFilterUiModel>,
    onFilterClick: (GameFilterUiModel) -> Unit,
    onGameClick: (Int) -> Unit,
    onSaveClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit,
    onClearFiltersClick: () -> Unit,
    state: LazyGridState,
    modifier: Modifier = Modifier
) {
    val chooseListLabel = stringResource(CoreUiRes.string.choose_list_content_description)
    val filtersHeader = @Composable {
        if (activeFilters.isNotEmpty()) {
            ActiveFiltersRow(
                filters = activeFilters,
                onFilterClick = onFilterClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (games.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) {
            filtersHeader()
            NoFilteredResultsPlaceholder(
                onClearFiltersClick = onClearFiltersClick,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = MaterialTheme.spacing.large)
            )
        }
    } else {
        // The grid's own space decides the columns, both the width and whether it counts as landscape: the window
        // would count the rail's space too, and a pane beside another one is portrait-shaped on a landscape window.
        BoxWithConstraints(modifier = modifier) {
            val edge = MaterialTheme.spacing.large
            val columnCount = resultGridColumnCount(
                availableWidth = maxWidth,
                spacing = MaterialTheme.spacing.large,
                edge = edge,
                isLandscape = isLandscapeShape(maxWidth, maxHeight)
            )

            LazyVerticalGrid(
                columns = SearchResultGridCells(columnCount, edge),
                state = state,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
                contentPadding = PaddingValues(bottom = ScrollToTopFabDefaults.ContentBottomPadding),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    filtersHeader()
                }

                itemsIndexed(
                    items = games,
                    key = { _, game -> game.id }
                ) { index, game ->
                    val column = index % columnCount

                    VerticalGameCard(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        onLongClickLabel = chooseListLabel,
                        onSaveClick = { onSaveClick(game.id) },
                        onLongClick = { onLongClick(game.id) },
                        modifier = Modifier
                            .animateItem(fadeOutSpec = null)
                            .padding(
                                start = if (column == 0) edge else 0.dp,
                                end = if (column == columnCount - 1) edge else 0.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    filters: List<GameFilterUiModel>,
    onFilterClick: (GameFilterUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    // Tapping an active filter removes it, so there is no chip left to bring into view.
    CustomChipRow(
        items = filters,
        key = { filter -> "${filter::class.simpleName}:${filter.id}" },
        modifier = modifier
    ) { gameFilter, _ ->
        CustomFilterChip(
            label = gameFilter.label.asString(),
            selected = true,
            onFilterClick = { onFilterClick(gameFilter) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(
                        Res.string.remove_filter_content_description,
                        gameFilter.label.asString()
                    ),
                    modifier = Modifier.requiredSize(FilterChipDefaults.IconSize)
                )
            }
        )
    }
}

private val previewFilters = listOf(
    GameFilterUiModel.Platform(id = 0, label = UiText.DynamicString("PC"), selected = true),
    GameFilterUiModel.Genre(id = 1, label = UiText.DynamicString("RPG"), selected = true)
)

@QuestLogPreviews
@Composable
private fun SearchResultGridPreview() {
    QuestLogTheme {
        SearchResultGrid(
            games = listOf(
                GameItemUiModel.getDummy(),
                GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"),
                GameItemUiModel.getDummy().copy(id = 3, name = "Elden Ring"),
                GameItemUiModel.getDummy().copy(id = 4, name = "Baldur's Gate 3")
            ),
            activeFilters = previewFilters,
            onFilterClick = {},
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onClearFiltersClick = {},
            state = rememberLazyGridState()
        )
    }
}

@Preview(widthDp = 800, heightDp = 360)
@Composable
private fun SearchResultGridLandscapePreview() {
    QuestLogTheme {
        SearchResultGrid(
            games = List(9) { index ->
                GameItemUiModel.getDummy().copy(id = index + 1, name = "Game ${index + 1}")
            },
            activeFilters = previewFilters,
            onFilterClick = {},
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onClearFiltersClick = {},
            state = rememberLazyGridState()
        )
    }
}

@QuestLogPreviews
@Composable
private fun SearchResultGridNoMatchPreview() {
    QuestLogTheme {
        SearchResultGrid(
            games = emptyList(),
            activeFilters = previewFilters,
            onFilterClick = {},
            onGameClick = {},
            onSaveClick = {},
            onLongClick = {},
            onClearFiltersClick = {},
            state = rememberLazyGridState()
        )
    }
}
