package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomModalBottomSheet
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.feature.search.model.SearchSort
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SortBottomSheetState
import com.example.gameswishlist.feature.search.model.SortingUiModel

/** Android's recommended minimum touch target size for accessibility. */
private val MIN_TOUCH_TARGET_HEIGHT = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSortBottomSheet(
    state: SortBottomSheetState,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isVisible) {
        CustomModalBottomSheet(
            onDismiss = { onEvent(SearchUiEvent.OnDismissSort) },
            modifier = modifier,
            title = stringResource(R.string.sort_label)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = MaterialTheme.spacing.extraLarge)
            ) {
                state.sorting.forEach { sortingUiModel ->
                    val isSelected = sortingUiModel.selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(SearchUiEvent.OnSortChanged(sortingUiModel)) }
                            .padding(
                                horizontal = MaterialTheme.spacing.large,
                                vertical = MaterialTheme.spacing.default
                            )
                            .heightIn(min = MIN_TOUCH_TARGET_HEIGHT),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sortingUiModel.label.asString(),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                imageVector = if (sortingUiModel.descending) Icons.Default.ArrowDownward
                                else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchSortBottomSheetPreview() {
    GamesWishlistTheme {
        SearchSortBottomSheet(
            state = SortBottomSheetState(
                isVisible = true,
                isSortActive = true,
                sorting = listOf(
                    SortingUiModel(
                        sortType = SearchSort.RELEVANCE,
                        label = UiText.DynamicString("Relevance"),
                        selected = false,
                        descending = true
                    ),
                    SortingUiModel(
                        sortType = SearchSort.NAME,
                        label = UiText.DynamicString("Name"),
                        selected = false,
                        descending = false
                    ),
                    SortingUiModel(
                        sortType = SearchSort.RATING,
                        label = UiText.DynamicString("Rating"),
                        selected = true,
                        descending = true
                    ),
                    SortingUiModel(
                        sortType = SearchSort.RELEASE_DATE,
                        label = UiText.DynamicString("Release date"),
                        selected = false,
                        descending = true
                    )
                )
            ),
            onEvent = {}
        )
    }
}
