package com.example.gameswishlist.feature.search.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomFilterChip
import com.example.gameswishlist.core.ui.component.CustomModalBottomSheet
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.feature.search.model.FilterBottomSheetState
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchUiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterBottomSheet(
    state: FilterBottomSheetState,
    onEvent: (SearchUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isVisible) {
        CustomModalBottomSheet(
            onDismiss = { onEvent(SearchUiEvent.OnDismissFilters) },
            modifier = modifier,
            title = stringResource(R.string.filters_title),
            titleTrailingContent = {
                TextButton(onClick = { onEvent(SearchUiEvent.OnClearFilters) }) {
                    Text(text = stringResource(R.string.clear_all))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = MaterialTheme.spacing.extraLarge)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    modifier = Modifier
                        .height(LocalWindowInfo.current.containerDpSize.height * 0.5f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Game Types Section
                    FilterSection(
                        title = stringResource(R.string.filter_section_game_type),
                        filters = state.filters.filterIsInstance<GameFilterUiModel.GameType>(),
                        onFilterClick = { onEvent(SearchUiEvent.OnBottomSheetFilterClick(it)) }
                    )
                    // Genres Section
                    FilterSection(
                        title = stringResource(R.string.filter_section_genres),
                        filters = state.filters.filterIsInstance<GameFilterUiModel.Genre>(),
                        onFilterClick = { onEvent(SearchUiEvent.OnBottomSheetFilterClick(it)) }
                    )
                    // Platforms Section
                    FilterSection(
                        title = stringResource(R.string.filter_section_platforms),
                        filters = state.filters.filterIsInstance<GameFilterUiModel.Platform>(),
                        onFilterClick = { onEvent(SearchUiEvent.OnBottomSheetFilterClick(it)) }
                    )
                }

                // Footer Apply Button
                Button(
                    onClick = { onEvent(SearchUiEvent.OnApplyFilters) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.large)
                ) {
                    Text(
                        text = stringResource(R.string.apply_filters_count, state.matchCount)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    filters: List<GameFilterUiModel>,
    onFilterClick: (GameFilterUiModel) -> Unit
) {
    if (filters.isEmpty()) return

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val maxRows = if (isExpanded) Int.MAX_VALUE else 3

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        FlowRow(
            modifier = Modifier
                .padding(horizontal = MaterialTheme.spacing.large)
                .fillMaxWidth()
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(-MaterialTheme.spacing.medium),
            maxItemsInEachRow = Int.MAX_VALUE,
            maxLines = maxRows
        ) {
            filters.forEach { filter ->
                CustomFilterChip(
                    label = filter.label.asString(),
                    onFilterClick = { onFilterClick(filter) },
                    selected = filter.selected
                )
            }
        }

        if (filters.size > 10) {
            TextButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
                ) {
                    Text(
                        text = if (isExpanded) stringResource(R.string.show_less)
                        else stringResource(R.string.show_more)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchFilterBottomSheetPreview() {
    GamesWishlistTheme {
        SearchFilterBottomSheet(
            state = FilterBottomSheetState(
                isVisible = true,
                matchCount = 124,
                filters = listOf(
                    GameFilterUiModel.GameType(0, UiText.DynamicString("Main Game"), true),
                    GameFilterUiModel.GameType(1, UiText.DynamicString("DLC"), false),
                    GameFilterUiModel.Genre(0, UiText.DynamicString("Action"), false),
                    GameFilterUiModel.Genre(1, UiText.DynamicString("RPG"), true),
                    GameFilterUiModel.Genre(2, UiText.DynamicString("Adventure"), false),
                    GameFilterUiModel.Genre(3, UiText.DynamicString("Shooter"), false),
                    GameFilterUiModel.Genre(4, UiText.DynamicString("Strategy"), false),
                    GameFilterUiModel.Genre(5, UiText.DynamicString("Simulation"), false),
                    GameFilterUiModel.Genre(6, UiText.DynamicString("Sports"), false),
                    GameFilterUiModel.Genre(7, UiText.DynamicString("Puzzle"), false),
                    GameFilterUiModel.Genre(8, UiText.DynamicString("Racing"), false),
                    GameFilterUiModel.Genre(9, UiText.DynamicString("Arcade"), false),
                    GameFilterUiModel.Genre(10, UiText.DynamicString("Indie"), false),
                    GameFilterUiModel.Genre(11, UiText.DynamicString("Casual"), false),
                    GameFilterUiModel.Platform(0, UiText.DynamicString("PC"), true),
                    GameFilterUiModel.Platform(1, UiText.DynamicString("PS5"), false),
                    GameFilterUiModel.Platform(2, UiText.DynamicString("Xbox"), false)
                )
            ),
            onEvent = {}
        )
    }
}
