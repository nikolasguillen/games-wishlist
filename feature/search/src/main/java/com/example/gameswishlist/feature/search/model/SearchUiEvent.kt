package com.example.gameswishlist.feature.search.model

internal sealed interface SearchUiEvent {
    data class OnSearchTriggered(val query: String) : SearchUiEvent
    data object OnClearSearch : SearchUiEvent
    data object OnClearHistory : SearchUiEvent
    data class OnHistoryItemRemoved(val query: String) : SearchUiEvent
    data class OnRecentGameRemoved(val gameId: Int) : SearchUiEvent
    data class OnFilterClick(val filter: GameFilterUiModel) : SearchUiEvent

    /** The user tapped a search result's save button, toggling its membership in the default wishlist. */
    data class OnToggleSave(val gameId: Int) : SearchUiEvent

    // Bottom Sheet Events
    data object OnOpenFilters : SearchUiEvent
    data object OnDismissFilters : SearchUiEvent
    data class OnBottomSheetFilterClick(val filter: GameFilterUiModel) : SearchUiEvent
    data object OnApplyFilters : SearchUiEvent
    data object OnClearFilters : SearchUiEvent
    data class OnSortChanged(val sort: SortingUiModel) : SearchUiEvent
    data object OnOpenSort : SearchUiEvent
    data object OnDismissSort : SearchUiEvent

    /** The user tapped the "refresh suggestions" prompt shown while the Discover feed is stale. */
    data object OnRefreshDiscover : SearchUiEvent

    /** The user closed the refresh prompt without acting on it. */
    data object OnDismissDiscoverRefresh : SearchUiEvent
}
