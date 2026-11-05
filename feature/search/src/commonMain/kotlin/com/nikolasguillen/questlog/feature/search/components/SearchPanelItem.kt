package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomFilterChip
import com.nikolasguillen.questlog.core.ui.component.gamecard.RecentGameCard
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.feature.search.model.GameSuggestionUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchDropdownItemUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchType

/** The width of the outline that marks the highlighted item. */
private val MARK_BORDER_WIDTH = 2.dp

/** How much of the content color tints the highlighted item. */
private const val MARK_TINT_ALPHA = 0.16f

/**
 * Makes one item of the wide search panel highlightable: it draws the mark while the item is [highlighted], highlights
 * the item when a mouse moves over it, and scrolls it into view when the keyboard highlights it.
 *
 * The mark is a tint with an outline, both in the theme's `onSurface` (light on a dark theme, dark on a light one), in
 * the item's own shape. It is the same for the keyboard and for the mouse, so the user always sees one highlight. It
 * is drawn over the item, because chips and the "See all results" pill are opaque and would hide a fill drawn behind
 * them. It is neutral on purpose: the primary color is also the tint of the genre chips, and a mark in it would not
 * show on them.
 *
 * Hover moves the highlight only when the pointer really moves: a pointer that rests still sends nothing, so it never
 * takes the highlight back from the keyboard (including when the list scrolls under it), and a touch never highlights.
 * It does not consume the events. The item is scrolled into view only when it is highlighted while the pointer is not
 * over it, since a hovered item is already under the user's hand and scrolling it would move the next one under it.
 *
 * @param onHighlight Asked to highlight [item] when the mouse moves over it.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.searchPanelItem(
    item: SearchDropdownItemUiModel,
    highlighted: SearchDropdownItemUiModel?,
    onHighlight: (SearchDropdownItemUiModel) -> Unit
): Modifier = composed {
    val isHighlighted = item == highlighted
    val currentOnHighlight by rememberUpdatedState(onHighlight)
    val requester = remember { BringIntoViewRequester() }
    var isPointerOver by remember { mutableStateOf(false) }

    LaunchedEffect(isHighlighted) {
        if (isHighlighted && !isPointerOver) requester.bringIntoView()
    }

    val shape = when (item) {
        is SearchDropdownItemUiModel.HistorySuggestion,
        is SearchDropdownItemUiModel.GameSuggestion,
        SearchDropdownItemUiModel.SeeAllResults -> MaterialTheme.shapes.medium

        is SearchDropdownItemUiModel.TypeChip,
        is SearchDropdownItemUiModel.Genre,
        SearchDropdownItemUiModel.ClearHistory,
        is SearchDropdownItemUiModel.RecentQuery,
        is SearchDropdownItemUiModel.RecentGame -> MaterialTheme.shapes.small
    }
    val outline = MaterialTheme.colorScheme.onSurface
    val tint = outline.copy(alpha = MARK_TINT_ALPHA)
    // The "See all results" row keeps its margin inside the modifier chain, so the mark is inset to its visible pill.
    val insetHorizontal = if (item == SearchDropdownItemUiModel.SeeAllResults) MaterialTheme.spacing.large else 0.dp
    val insetVertical = if (item == SearchDropdownItemUiModel.SeeAllResults) MaterialTheme.spacing.medium else 0.dp

    this
        .bringIntoViewRequester(requester)
        .pointerInput(item) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: continue
                    if (change.type != PointerType.Mouse) continue
                    when (event.type) {
                        PointerEventType.Exit -> isPointerOver = false
                        PointerEventType.Enter, PointerEventType.Move -> {
                            if (change.positionChanged() && !change.changedToUp()) {
                                isPointerOver = true
                                currentOnHighlight(item)
                            }
                        }
                    }
                }
            }
        }
        .drawWithContent {
            if (isHighlighted) {
                val horizontal = insetHorizontal.toPx()
                val vertical = insetVertical.toPx()
                drawContent()
                inset(horizontal = horizontal, vertical = vertical) {
                    val markShape = shape.createOutline(size, layoutDirection, this)
                    drawOutline(markShape, color = tint)
                    drawOutline(
                        outline = markShape,
                        color = outline,
                        style = Stroke(width = MARK_BORDER_WIDTH.toPx())
                    )
                }
            } else {
                drawContent()
            }
        }
}

@Composable
private fun PreviewPanel(content: @Composable () -> Unit) {
    QuestLogTheme {
        Surface(color = MaterialTheme.appColors.expandedSearchBarColor) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) { content() }
        }
    }
}

@QuestLogPreviews
@Composable
private fun SearchPanelItemChipPreview() {
    val chip = SearchDropdownItemUiModel.TypeChip(SearchType.GAMES)
    PreviewPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)) {
            Box(Modifier.searchPanelItem(chip, highlighted = chip, onHighlight = {})) {
                CustomFilterChip(label = "Games", selected = true, onFilterClick = {})
            }
            Box(Modifier.searchPanelItem(chip, highlighted = null, onHighlight = {})) {
                CustomFilterChip(label = "Games", selected = true, onFilterClick = {})
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun SearchPanelItemGameCardPreview() {
    val game = GameItemUiModel.getDummy()
    val item = SearchDropdownItemUiModel.RecentGame(game.id)
    PreviewPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)) {
            Box(Modifier.searchPanelItem(item, highlighted = item, onHighlight = {})) {
                RecentGameCard(game = game, onClick = {}, onRemoveClick = {})
            }
            Box(Modifier.searchPanelItem(item, highlighted = null, onHighlight = {})) {
                RecentGameCard(game = game, onClick = {}, onRemoveClick = {})
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun SearchPanelItemSuggestionRowsPreview() {
    val suggestion = GameSuggestionUiModel(1, "Cyberpunk 2077", null, "CD Projekt Red · 2020")
    val game = SearchDropdownItemUiModel.GameSuggestion(suggestion.id)
    val seeAll = SearchDropdownItemUiModel.SeeAllResults
    PreviewPanel {
        GameSuggestionRow(
            suggestion = suggestion,
            onClick = {},
            modifier = Modifier.fillMaxWidth().searchPanelItem(game, highlighted = game, onHighlight = {})
        )
        GameSuggestionRow(
            suggestion = suggestion,
            onClick = {},
            modifier = Modifier.fillMaxWidth().searchPanelItem(game, highlighted = null, onHighlight = {})
        )
        SeeAllResultsRow(
            query = "cyberpunk",
            onClick = {},
            modifier = Modifier.fillMaxWidth().searchPanelItem(seeAll, highlighted = seeAll, onHighlight = {})
        )
        SeeAllResultsRow(
            query = "cyberpunk",
            onClick = {},
            modifier = Modifier.fillMaxWidth().searchPanelItem(seeAll, highlighted = null, onHighlight = {})
        )
    }
}
