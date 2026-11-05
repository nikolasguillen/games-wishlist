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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomChipRow
import com.nikolasguillen.questlog.core.ui.component.CustomTextButton
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.clear_all
import com.nikolasguillen.questlog.feature.search.resources.recent_searches
import org.jetbrains.compose.resources.stringResource

/**
 * The recent searches, under a heading with a "clear all" button.
 *
 * @param wrap Lay the chips out in a wrapping grid instead of one row that scrolls sideways.
 * @param clearAllModifier Applied to a box around the "clear all" button, so a caller can mark or track it.
 * @param chipModifier Applied to a box around each chip, given its query, for the same purpose.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecentSearchesSection(
    recentSearches: List<String>,
    wrap: Boolean,
    onClearRecentSearches: () -> Unit,
    onHistoryItemClicked: (String) -> Unit,
    onShowRemovalDialog: (String) -> Unit,
    clearAllModifier: Modifier = Modifier,
    chipModifier: (String) -> Modifier = { Modifier }
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

            Box(modifier = clearAllModifier) {
                CustomTextButton(
                    onClick = onClearRecentSearches,
                ) {
                    Text(stringResource(Res.string.clear_all))
                }
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
                    Box(modifier = chipModifier(recentSearch)) {
                        RecentSearchChip(
                            recentSearch = recentSearch,
                            onClick = { onHistoryItemClicked(recentSearch) },
                            onLongClick = { onShowRemovalDialog(recentSearch) }
                        )
                    }
                }
            }
        } else {
            CustomChipRow(
                items = recentSearches,
                key = { it.hashCode() }
            ) { recentSearch, _ ->
                Box(modifier = chipModifier(recentSearch)) {
                    RecentSearchChip(
                        recentSearch = recentSearch,
                        onClick = { onHistoryItemClicked(recentSearch) },
                        onLongClick = { onShowRemovalDialog(recentSearch) }
                    )
                }
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

private val previewSearches = listOf(
    "The Witcher", "Cyberpunk 2077", "Hollow Knight", "Elden Ring", "Baldur's Gate 3", "Zelda"
)

@QuestLogPreviews
@Composable
private fun RecentSearchesSectionPreview() {
    QuestLogTheme {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)) {
            RecentSearchesSection(
                recentSearches = previewSearches,
                wrap = true,
                onClearRecentSearches = {},
                onHistoryItemClicked = {},
                onShowRemovalDialog = {}
            )
            RecentSearchesSection(
                recentSearches = previewSearches,
                wrap = false,
                onClearRecentSearches = {},
                onHistoryItemClicked = {},
                onShowRemovalDialog = {}
            )
        }
    }
}

@Preview(widthDp = 720)
@Composable
private fun RecentSearchesSectionWidePreview() {
    QuestLogTheme {
        RecentSearchesSection(
            recentSearches = previewSearches,
            wrap = true,
            onClearRecentSearches = {},
            onHistoryItemClicked = {},
            onShowRemovalDialog = {}
        )
    }
}
