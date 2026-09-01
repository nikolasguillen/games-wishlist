package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * Main UI state for the Search screen.
 *
 * The screen has two independent content areas, so it carries two content states rather than one.
 * [contentState] is authoritative about which is on screen: while it is [SearchContentState.Idle] the
 * feed in [discover] is rendered, and any other value means a committed search holds the content area.
 *
 * @property history Aggregated user search history activity.
 * @property suggestions Composite state for search suggestions (Local history & Remote games).
 * @property discover The Discover feed, kept current whether or not it is the thing on screen.
 * @property contentState Current state of the search results (Idle, Loading, Success, Empty, Error).
 * @property filtersBottomSheetState State of the filters selection Bottom Sheet.
 * @property sortBottomSheetState State for the sorting options Bottom Sheet.
 */
@Immutable
internal data class SearchUiState(
    val history: SearchHistoryUiModel = SearchHistoryUiModel(),
    val suggestions: SearchSuggestionsUiModel = SearchSuggestionsUiModel(),
    val discover: DiscoverContentState = DiscoverContentState.Loading,
    val contentState: SearchContentState = SearchContentState.Idle,
    val filtersBottomSheetState: FilterBottomSheetState = FilterBottomSheetState(),
    val sortBottomSheetState: SortBottomSheetState = SortBottomSheetState()
)
