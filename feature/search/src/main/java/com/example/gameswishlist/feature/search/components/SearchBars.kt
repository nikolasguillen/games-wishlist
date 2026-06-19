package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AppBarWithSearchColors
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.feature.search.R as SearchR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsedSearchBar(
    searchBarState: SearchBarState,
    scrollBehavior: SearchBarScrollBehavior,
    appBarWithSearchColors: AppBarWithSearchColors,
    inputField: @Composable () -> Unit
) {
    AppBarWithSearch(
        scrollBehavior = scrollBehavior,
        state = searchBarState,
        colors = appBarWithSearchColors,
        inputField = inputField,
        contentPadding = PaddingValues(
            bottom = MaterialTheme.spacing.large,
            top = WindowInsets.statusBars.asPaddingValues()
                .calculateTopPadding() + MaterialTheme.spacing.large
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedSearchBar(
    searchBarState: SearchBarState,
    inputField: @Composable () -> Unit,
    recentSearches: List<String>,
    appBarWithSearchColors: AppBarWithSearchColors,
    onHistoryItemClicked: (query: String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onRemoveRecentSearchItem: (query: String) -> Unit
) {
    var showHistoryItemRemovalDialog by remember { mutableStateOf(false) }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        colors = appBarWithSearchColors.searchBarColors.copy(
            containerColor = MaterialTheme.appColors.expandedSearchBarColor
        )
    ) {
        if (recentSearches.isEmpty()) return@ExpandedFullScreenSearchBar
        var recentSearchToBeRemoved by remember { mutableStateOf("") }

        fun showRecentSearchRemovalDialog(itemToRemove: String) {
            recentSearchToBeRemoved = itemToRemove
            showHistoryItemRemovalDialog = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = MaterialTheme.spacing.medium
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.large)
            ) {
                Text(
                    text = stringResource(SearchR.string.recent_searches),
                    style = MaterialTheme.typography.titleMedium
                )

                TextButton(
                    onClick = onClearRecentSearches
                ) {
                    Text(
                        text = stringResource(SearchR.string.clear_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.appColors.onAppBackground
                    )
                }
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
            ) {
                items(items = recentSearches, key = { it }) { recentSearch ->
                    val inputChipInteractionSource = remember { MutableInteractionSource() }
                    Box {
                        SuggestionChip(
                            onClick = { onHistoryItemClicked(recentSearch) },
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
                            interactionSource = inputChipInteractionSource
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .combinedClickable(
                                    onLongClick = { showRecentSearchRemovalDialog(recentSearch) },
                                    onClick = { onHistoryItemClicked(recentSearch) },
                                    interactionSource = inputChipInteractionSource,
                                    indication = null,
                                )
                        )
                    }
                }
            }
        }

        if (showHistoryItemRemovalDialog) {
            CustomAlertDialog(
                title = stringResource(SearchR.string.remove_history_item),
                message = stringResource(
                    SearchR.string.remove_history_item_message,
                    recentSearchToBeRemoved
                ),
                confirmButtonText = stringResource(SearchR.string.proceed_label),
                onConfirm = { onRemoveRecentSearchItem(recentSearchToBeRemoved) },
                dismissButtonText = stringResource(SearchR.string.cancel),
                onDismiss = { showHistoryItemRemovalDialog = false }
            )
        }
    }
}
