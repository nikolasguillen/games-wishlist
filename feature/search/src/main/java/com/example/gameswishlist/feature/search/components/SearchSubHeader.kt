package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.util.shimmerEffect
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchSort
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState

@Composable
internal fun SearchSubHeader(
    uiState: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = MaterialTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (uiState.contentState) {
            is SearchContentState.Success -> {
                Text(
                    text = stringResource(
                        R.string.search_results_count,
                        uiState.contentState.games.size
                    ),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val currentSort = uiState.sortBottomSheetState.selectedSorting

                    // Sort icon is now static. Tint changes if not (Relevance AND Descending).
                    val isDefaultSort = currentSort == null ||
                            (currentSort.sortType == SearchSort.RELEVANCE && currentSort.descending)

                    IconButton(onClick = { onEvent(SearchUiEvent.OnOpenSort) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_label),
                            tint = if (isDefaultSort) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }

                    IconButton(onClick = { onEvent(SearchUiEvent.OnOpenFilters) }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.filter_label)
                        )
                    }
                }
            }

            SearchContentState.Loading -> {
                // Games count shimmer
                Box(
                    modifier = Modifier
                        .padding(vertical = MaterialTheme.spacing.medium)
                        .width(120.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(MaterialTheme.spacing.medium))
                        .shimmerEffect()
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
            }
        }
    }
}
