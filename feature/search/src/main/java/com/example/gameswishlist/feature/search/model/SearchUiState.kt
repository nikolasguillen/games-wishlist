package com.example.gameswishlist.feature.search.model

/**
 * Main UI state for the Search screen.
 *
 * @property history Aggregated user search history activity.
 * @property contentState Current state of the search results (Loading, Success, Empty, etc.).
 * @property filtersBottomSheetState State of the filters selection Bottom Sheet.
 * @property sortBottomSheetState State for the sorting options Bottom Sheet.
 */
data class SearchUiState(
    val history: SearchHistoryUiModel = SearchHistoryUiModel(),
    val suggestions: List<String> = emptyList(),
    val contentState: SearchContentState = SearchContentState.Initial,
    val filtersBottomSheetState: FilterBottomSheetState = FilterBottomSheetState(),
    val sortBottomSheetState: SortBottomSheetState = SortBottomSheetState()
)