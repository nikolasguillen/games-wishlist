package com.example.gameswishlist.feature.search.model

/**
 * Represents the state of the sorting options Bottom Sheet.
 * @property isVisible Whether the Bottom Sheet is currently visible.
 * @property sorting List of sorting options.
 * @property isSortActive Whether any non-default sorting is currently active.
 */
internal data class SortBottomSheetState(
    val isVisible: Boolean = false,
    val sorting: List<SortingUiModel> = emptyList(),
    val isSortActive: Boolean = false
) {
    /**
     * Returns the currently selected sorting option.
     */
    val selectedSorting: SortingUiModel?
        get() = sorting.find { it.selected }
}
