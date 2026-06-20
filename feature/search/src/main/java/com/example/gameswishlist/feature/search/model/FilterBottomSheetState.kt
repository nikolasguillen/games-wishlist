package com.example.gameswishlist.feature.search.model

/**
 * State representing the temporary filter selection within the Bottom Sheet.
 * 
 * @property isVisible Whether the bottom sheet is currently displayed.
 * @property filters The list of filters with their temporary selection state.
 * @property matchCount Number of games from the current result set that match these filters.
 */
data class FilterBottomSheetState(
    val isVisible: Boolean = false,
    val filters: List<GameFilterUiModel> = emptyList(),
    val matchCount: Int = 0
)
