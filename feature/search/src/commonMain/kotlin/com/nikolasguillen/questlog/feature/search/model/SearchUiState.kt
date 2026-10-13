package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable

/**
 * Main UI state for the Search screen.
 *
 * The screen has two independent content areas, so it carries two content states rather than one.
 * [contentState] is authoritative about which is on screen: while it is [SearchContentState.Idle] the
 * feed in [discover] is rendered, and any other value means a committed search holds the content area.
 *
 * @property browseGenres The "Browse by genre" chip row, always five genres once the ViewModel has seeded it.
 * Empty only in a bare [SearchUiState] (previews, tests), where the section is not drawn.
 * @property activeBrowse What the committed grid is showing when it is not a typed query -- a genre browse or
 * a Discover shelf -- or `null` when neither is active. A browse renders through [contentState] like a text
 * search, so two invariants hold: `activeBrowse != null` implies `contentState` is not
 * [SearchContentState.Idle], and `activeBrowse is SearchBrowseUiModel.Shelf` implies `contentState` is
 * [SearchContentState.Success] (a shelf grid is built from games already in memory, so it has no loading or
 * error state, and nothing that could write one is left running while it is open).
 * @property history Aggregated user search history activity.
 * @property suggestions Composite state for search suggestions (Local history & Remote games).
 * @property discover The Discover feed, kept current whether or not it is the thing on screen.
 * @property contentState Current state of the search results (Idle, Loading, Success, Empty, Error).
 * @property filtersBottomSheetState State of the filters selection Bottom Sheet.
 * @property sortBottomSheetState State for the sorting options Bottom Sheet.
 * @property listSelectorState State of the list-selector bottom sheet; `null` means it is closed.
 */
@Immutable
internal data class SearchUiState(
    val browseGenres: List<BrowseGenreUiModel> = emptyList(),
    val activeBrowse: SearchBrowseUiModel? = null,
    val history: SearchHistoryUiModel = SearchHistoryUiModel(),
    val suggestions: SearchSuggestionsUiModel = SearchSuggestionsUiModel(),
    val discover: DiscoverContentState = DiscoverContentState.Loading,
    val contentState: SearchContentState = SearchContentState.Idle,
    val filtersBottomSheetState: FilterBottomSheetState = FilterBottomSheetState(),
    val sortBottomSheetState: SortBottomSheetState = SortBottomSheetState(),
    val listSelectorState: ListSelectorState? = null
)
