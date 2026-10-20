package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.feature.search.model.SearchType
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.filter_label
import com.nikolasguillen.questlog.feature.search.resources.search_characters_count
import com.nikolasguillen.questlog.feature.search.resources.search_results_count
import com.nikolasguillen.questlog.feature.search.resources.sort_label
import org.jetbrains.compose.resources.stringResource

/**
 * The count of what is on screen, with the sort and filter buttons when there is something to sort or filter.
 *
 * Characters and developers have neither, so a `null` [onOpenSort] or [onOpenFilters] leaves that button out.
 * [resultsType] picks the wording of the count.
 */
@Composable
internal fun SearchSubHeader(
    resultsCount: Int,
    isSortActive: Boolean,
    onOpenSort: (() -> Unit)?,
    onOpenFilters: (() -> Unit)?,
    modifier: Modifier = Modifier,
    resultsType: SearchType = SearchType.GAMES
) {
    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(
                when (resultsType) {
                    SearchType.GAMES -> Res.string.search_results_count
                    SearchType.CHARACTERS -> Res.string.search_characters_count
                    SearchType.DEVELOPERS -> Res.string.search_results_count
                },
                resultsCount
            ),
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onOpenSort != null) {
                IconButton(onClick = onOpenSort) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(Res.string.sort_label),
                        tint = if (isSortActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }

            if (onOpenFilters != null) {
                IconButton(onClick = onOpenFilters) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(Res.string.filter_label),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun SearchSubHeaderCharactersPreview() {
    QuestLogTheme {
        SearchSubHeader(
            resultsCount = 2,
            isSortActive = false,
            onOpenSort = null,
            onOpenFilters = null,
            resultsType = SearchType.CHARACTERS
        )
    }
}

@QuestLogPreviews
@Composable
private fun SearchSubHeaderPreview() {
    QuestLogTheme {
        SearchSubHeader(
            resultsCount = 42,
            isSortActive = true,
            onOpenSort = {},
            onOpenFilters = {}
        )
    }
}
