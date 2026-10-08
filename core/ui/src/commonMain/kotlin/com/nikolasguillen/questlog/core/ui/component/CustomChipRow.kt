package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.util.modifiers.fadingEdgeHorizontal
import kotlinx.coroutines.launch

/**
 * A horizontally scrolling row of chips that fades out at the edges with more content behind them.
 *
 * The row owns the layout, the fade and the scrolling; [chip] draws each item and decides what a click does.
 * It also receives a `bringIntoView` callback that scrolls the row just far enough for that chip to sit clear
 * of the fade. Call it when the chip stays in the row after the click (a selection change); skip it when the
 * click removes the chip, since there is nothing left to reveal.
 *
 * @param items The items to show, one chip each.
 * @param key A stable, saveable key for each item.
 * @param state The scroll state of the row.
 * @param chip Draws the chip for an item, given a callback that brings it fully into view.
 */
@Composable
fun <T> CustomChipRow(
    items: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    chip: @Composable (item: T, bringIntoView: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    val fadeSize = MaterialTheme.spacing.doubleLarge
    val fadePx = with(LocalDensity.current) { fadeSize.toPx() }

    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
        modifier = modifier.fadingEdgeHorizontal(
            state = state,
            maxFadeSize = fadeSize,
            rampDistance = fadeSize * 1.5f
        )
    ) {
        items(items = items, key = key) { item ->
            val requester = remember { BringIntoViewRequester() }
            // Read only inside the click callback, so updating it never recomposes the chip.
            val chipSize = remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .bringIntoViewRequester(requester)
                    .onSizeChanged { chipSize.value = it }
            ) {
                chip(item) {
                    // Widen the requested area by the fade on both sides, so the chip lands where it is
                    // fully opaque. Scrolling clamps at the row's ends, where there is no fade to clear.
                    val size = chipSize.value
                    scope.launch {
                        requester.bringIntoView(
                            Rect(
                                left = -fadePx,
                                top = 0f,
                                right = size.width + fadePx,
                                bottom = size.height.toFloat()
                            )
                        )
                    }
                }
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun CustomChipRowPreview() {
    QuestLogTheme {
        CustomChipRow(
            items = listOf("Action", "RPG", "Strategy", "Platformer", "Shooter", "Puzzle"),
            key = { it }
        ) { label, bringIntoView ->
            CustomFilterChip(
                label = label,
                selected = label == "RPG",
                onFilterClick = bringIntoView
            )
        }
    }
}
