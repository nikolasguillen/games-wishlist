package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import com.nikolasguillen.questlog.core.ui.component.CustomFilterChip
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.util.modifiers.fadingEdgeHorizontal
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistFilterChipUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistStatusFilter
import kotlinx.coroutines.launch

/**
 * The slim row between the list header and the first status section: the status filter chips on the left and
 * the view toggle at the end.
 *
 * The chip strip always takes the space left of the toggle, even when [chips] is empty, so the toggle stays at
 * the end of the row and many chips scroll instead of pushing it off-screen.
 */
@Composable
internal fun WishlistViewOptionsRow(
    viewMode: WishlistViewMode,
    chips: List<WishlistFilterChipUiModel>,
    onChipClick: (WishlistStatusFilter) -> Unit,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(end = MaterialTheme.spacing.medium)
    ) {
        LazyRow(
            state = state,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large),
            modifier = Modifier
                .weight(1f)
                .fadingEdgeHorizontal(
                    state = state,
                    maxFadeSize = MaterialTheme.spacing.doubleLarge,
                    rampDistance = MaterialTheme.spacing.doubleLarge * 1.5f
                )
        ) {
            items(items = chips, key = { chip -> chip.filter.key() }) { chip ->
                CustomFilterChip(
                    label = chip.label.asString(),
                    selected = chip.isSelected,
                    onFilterClick = {
                        onChipClick(chip.filter)
                        val itemInfo = state.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.index == chips.indexOf(chip) }
                        val isFullyVisible = itemInfo != null &&
                                itemInfo.offset >= state.layoutInfo.viewportStartOffset &&
                                itemInfo.offset + itemInfo.size <= state.layoutInfo.viewportEndOffset

                        if (itemInfo == null || !isFullyVisible) {
                            scope.launch { state.animateScrollToItem(chips.indexOf(chip)) }
                        }
                    }
                )
            }
        }
        WishlistViewModeToggle(viewMode = viewMode, onClick = onToggleClick)
    }
}

/** Lazy item keys have to be saveable on Android, which the filter objects themselves are not. */
private fun WishlistStatusFilter.key(): String = when (this) {
    WishlistStatusFilter.All -> "all"
    is WishlistStatusFilter.Only -> "only_${status?.id ?: "none"}"
}

private val previewChips = listOf(
    WishlistFilterChipUiModel(
        WishlistStatusFilter.All,
        UiText.DynamicString("All"),
        isSelected = false
    ),
    WishlistFilterChipUiModel(
        WishlistStatusFilter.Only(GameStatus.PLAYING),
        UiText.DynamicString("Playing"),
        isSelected = true
    ),
    WishlistFilterChipUiModel(
        WishlistStatusFilter.Only(GameStatus.COMPLETED),
        UiText.DynamicString("Completed"),
        isSelected = false
    ),
    WishlistFilterChipUiModel(
        WishlistStatusFilter.Only(null),
        UiText.DynamicString("No status"),
        isSelected = false
    )
)

@QuestLogPreviews
@Composable
private fun WishlistViewOptionsRowPreview() {
    QuestLogTheme {
        WishlistViewOptionsRow(
            viewMode = WishlistViewMode.LIST,
            chips = previewChips,
            onChipClick = {},
            onToggleClick = {}
        )
    }
}