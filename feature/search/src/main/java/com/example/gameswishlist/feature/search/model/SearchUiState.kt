package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * Main UI state for the Search screen.
 *
 * @property history Aggregated user search history activity.
 * @property suggestions Composite state for search suggestions (Local history & Remote games).
 * @property contentState Current state of the search results (Loading, Success, Empty, etc.).
 * @property filtersBottomSheetState State of the filters selection Bottom Sheet.
 * @property sortBottomSheetState State for the sorting options Bottom Sheet.
 */
@Immutable
internal data class SearchUiState(
    val history: SearchHistoryUiModel = SearchHistoryUiModel(),
    val suggestions: SearchSuggestionsUiModel = SearchSuggestionsUiModel(),
    val contentState: SearchContentState = SearchContentState.Discover,
    val filtersBottomSheetState: FilterBottomSheetState = FilterBottomSheetState(),
    val sortBottomSheetState: SortBottomSheetState = SortBottomSheetState()
)
