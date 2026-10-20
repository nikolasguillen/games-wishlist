package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.ScrollToTopFabDefaults
import com.nikolasguillen.questlog.core.ui.util.isLandscapeShape
import com.nikolasguillen.questlog.feature.search.model.EntityResultUiModel

/**
 * The grid of a Characters or Developers search, with the same columns and edges as the games grid so the page
 * does not change shape when the type does. [card] draws one result; it gets the modifier that places it in its
 * cell, which it has to apply to the card.
 */
@Composable
internal fun EntityResultGrid(
    items: List<EntityResultUiModel>,
    state: LazyGridState,
    modifier: Modifier = Modifier,
    card: @Composable (item: EntityResultUiModel, modifier: Modifier) -> Unit
) {
    // The grid's own space decides the columns, as in SearchResultGrid.
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
            contentPadding = PaddingValues(
                top = MaterialTheme.spacing.large,
                bottom = ScrollToTopFabDefaults.ContentBottomPadding
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> item.id }
            ) { index, item ->
                val column = index % columnCount

                card(
                    item,
                    Modifier
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

@QuestLogPreviews
@Composable
private fun EntityResultGridPreview() {
    QuestLogTheme {
        EntityResultGrid(
            items = List(6) { EntityResultUiModel(id = it, name = "Character $it", imageUrl = null) },
            state = rememberLazyGridState()
        ) { item, cardModifier ->
            CharacterResultCard(item = item, onClick = {}, modifier = cardModifier)
        }
    }
}
