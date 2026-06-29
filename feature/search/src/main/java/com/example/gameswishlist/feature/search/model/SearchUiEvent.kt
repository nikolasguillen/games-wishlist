package com.example.gameswishlist.feature.search.model

sealed interface SearchUiEvent {
    data class OnSearchTriggered(val query: String) : SearchUiEvent
    data class OnSuggestionClick(val suggestion: SearchSuggestionUiModel) : SearchUiEvent
    data object OnClearHistory : SearchUiEvent
    data class OnHistoryItemRemoved(val query: String) : SearchUiEvent
    data class OnRecentGameRemoved(val gameId: Int) : SearchUiEvent
    data class OnFilterClick(val filter: GameFilterUiModel) : SearchUiEvent

    // Bottom Sheet Events
    data object OnOpenFilters : SearchUiEvent
    data object OnDismissFilters : SearchUiEvent
    data class OnBottomSheetFilterClick(val filter: GameFilterUiModel) : SearchUiEvent
    data object OnApplyFilters : SearchUiEvent
    data object OnClearFilters : SearchUiEvent
    data class OnSortChanged(val sort: SortingUiModel) : SearchUiEvent
    data object OnOpenSort : SearchUiEvent
    data object OnDismissSort : SearchUiEvent
}