package com.nikolasguillen.questlog.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.common.calculateGameRelevanceScore
import com.nikolasguillen.questlog.core.domain.usecase.ToggleWishlistUseCase
import com.nikolasguillen.questlog.core.domain.usecase.discover.GetDiscoverFeedUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.AddGameToListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistedGameIdsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.BrowseGenreUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.ClearAllHistoryUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.ClearRecentGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetBrowseGenresUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetRecentSearchActivityUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.RemoveRecentGameUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.SearchGamesUseCase
import com.nikolasguillen.questlog.core.domain.usecase.search.toSearchResult
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.DiscoverFeed
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.SearchSuggestion
import com.nikolasguillen.questlog.core.ui.mapper.getDisplayRating
import com.nikolasguillen.questlog.core.ui.mapper.toGameItemList
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.search.mapper.defaultBrowseGenres
import com.nikolasguillen.questlog.feature.search.mapper.getInitialGameTypeFilters
import com.nikolasguillen.questlog.feature.search.mapper.getInitialSortFilters
import com.nikolasguillen.questlog.feature.search.mapper.isSortActive
import com.nikolasguillen.questlog.feature.search.mapper.shelfGames
import com.nikolasguillen.questlog.feature.search.mapper.toBrowseGenreUiModels
import com.nikolasguillen.questlog.feature.search.mapper.toDiscoverContentState
import com.nikolasguillen.questlog.feature.search.mapper.toGenreFilters
import com.nikolasguillen.questlog.feature.search.mapper.toPlatformFilters
import com.nikolasguillen.questlog.feature.search.mapper.toSelectorItem
import com.nikolasguillen.questlog.feature.search.mapper.toSuggestionUiModels
import com.nikolasguillen.questlog.feature.search.mapper.withRelevanceLabel
import com.nikolasguillen.questlog.feature.search.model.BrowseGenreUiModel
import com.nikolasguillen.questlog.feature.search.model.DiscoverContentState
import com.nikolasguillen.questlog.feature.search.model.DiscoverShelfUiModel
import com.nikolasguillen.questlog.feature.search.model.FilterBottomSheetState
import com.nikolasguillen.questlog.feature.search.model.GameFilterUiModel
import com.nikolasguillen.questlog.feature.search.model.ListSelectorState
import com.nikolasguillen.questlog.feature.search.model.SearchBrowseUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchContentState
import com.nikolasguillen.questlog.feature.search.model.SearchHistoryUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchSort
import com.nikolasguillen.questlog.feature.search.model.SearchSuggestionsUiModel
import com.nikolasguillen.questlog.feature.search.model.SearchUiEffect
import com.nikolasguillen.questlog.feature.search.model.SearchUiEvent
import com.nikolasguillen.questlog.feature.search.model.SearchUiState
import com.nikolasguillen.questlog.feature.search.model.SortBottomSheetState
import com.nikolasguillen.questlog.feature.search.model.SortingUiModel
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.added_to_wishlist
import com.nikolasguillen.questlog.feature.search.resources.removed_from_wishlist
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val SUGGESTIONS_DEBOUNCE = 300.milliseconds

/**
 * Two characters match too much to be useful: the remote suggestion query is a substring match on the
 * title, so "ze" returns noise for a request IGDB rate-limits on a shared credential.
 */
private const val MIN_SUGGESTION_QUERY_LENGTH = 3

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchGamesUseCase: SearchGamesUseCase,
    private val browseGenreUseCase: BrowseGenreUseCase,
    private val getBrowseGenresUseCase: GetBrowseGenresUseCase,
    private val addSearchToHistoryUseCase: AddSearchToHistoryUseCase,
    private val getRecentSearchActivityUseCase: GetRecentSearchActivityUseCase,
    private val deleteSearchHistoryItemUseCase: DeleteSearchHistoryItemUseCase,
    private val clearAllHistoryUseCase: ClearAllHistoryUseCase,
    private val removeRecentGameUseCase: RemoveRecentGameUseCase,
    private val clearRecentGamesUseCase: ClearRecentGamesUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase,
    private val getDiscoverFeedUseCase: GetDiscoverFeedUseCase,
    private val getWishlistedGameIdsUseCase: GetWishlistedGameIdsUseCase,
    private val toggleWishlistUseCase: ToggleWishlistUseCase,
    private val getWishlistAssignmentsUseCase: GetWishlistAssignmentsUseCase,
    private val addGameToListUseCase: AddGameToListUseCase,
    private val removeGameFromListUseCase: RemoveGameFromListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            browseGenres = defaultBrowseGenres(),
            sortBottomSheetState = SortBottomSheetState(
                sorting = getInitialSortFilters(),
                isSortActive = false // Initial is always Relevance/Desc
            )
        )
    )
    internal val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SearchUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    internal val textFieldState = TextFieldState()

    // Forces initSearchSuggestions' collectLatest to cancel any in-flight suggestions
    // fetch even when textFieldState's value doesn't change (e.g. committing a search
    // for the exact text that's already typed).
    private val suggestionsResetTrigger = MutableSharedFlow<String>(extraBufferCapacity = 1)

    // The user asking for updated recommendations from a stale feed. Same shape as the trigger above,
    // for the same reason: it has no meaningful value at rest, only the fact that a tap happened.
    private val discoverRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Cached alongside the collector below so a freshly built SearchContentState.Success (a new search,
    // a filter or sort change) can be stamped with membership synchronously, instead of starting every
    // card unsaved for one frame until the collector's next emission patches it.
    private val wishlistedGameIds = MutableStateFlow<Set<Int>>(emptySet())

    // The domain feed behind the currently rendered Discover state, so a save/long-press coming from one
    // of its cards can resolve back to a Game the same way a search result's card does -- the Discover
    // feed is shown while contentState is Idle, so it has no SearchContentState.Success.allGames to search.
    private var discoverFeed: DiscoverFeed? = null

    private var searchJob: Job? = null

    init {
        initSearchHistory()
        initSearchSuggestions()
        observeBrowseGenres()
        observeDiscoverFeed()
        observeWishlistedGameIds()
    }

    internal fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnSearchTriggered -> {
                textFieldState.setTextAndPlaceCursorAtEnd(event.query)
                performSearch(query = event.query)
            }

            is SearchUiEvent.OnGenreBrowseTriggered -> {
                browseGenre(event.genre)
            }

            is SearchUiEvent.OnShelfSeeAllTriggered -> {
                openShelf(event.shelf)
            }

            SearchUiEvent.OnClearSearch -> {
                clearSearch()
            }

            SearchUiEvent.OnClearHistory -> {
                viewModelScope.launch {
                    clearAllHistoryUseCase()
                    clearRecentGamesUseCase()
                }
            }

            is SearchUiEvent.OnHistoryItemRemoved -> {
                viewModelScope.launch {
                    deleteSearchHistoryItemUseCase(event.query)
                }
            }

            is SearchUiEvent.OnRecentGameRemoved -> {
                viewModelScope.launch {
                    removeRecentGameUseCase(event.gameId)
                }
            }

            is SearchUiEvent.OnFilterClick -> {
                handleFilterClick(event.filter)
            }

            is SearchUiEvent.OnToggleSave -> {
                toggleSave(event.gameId)
            }

            is SearchUiEvent.OnOpenListSelector -> {
                openListSelector(event.gameId)
            }

            is SearchUiEvent.OnToggleListSelection -> {
                toggleListSelection(event.listId)
            }

            SearchUiEvent.OnConfirmListSelection -> {
                confirmListSelection()
            }

            SearchUiEvent.OnDismissListSelector -> {
                _uiState.update { it.copy(listSelectorState = null) }
            }

            SearchUiEvent.OnOpenFilters -> {
                val contentState = _uiState.value.contentState
                if (contentState !is SearchContentState.Success) return

                _uiState.update {
                    it.copy(
                        filtersBottomSheetState = FilterBottomSheetState(
                            isVisible = true,
                            filters = contentState.filters,
                            matchCount = contentState.games.size
                        )
                    )
                }
            }

            SearchUiEvent.OnDismissFilters -> {
                _uiState.update {
                    it.copy(
                        filtersBottomSheetState = it.filtersBottomSheetState.copy(
                            isVisible = false
                        )
                    )
                }
            }

            is SearchUiEvent.OnBottomSheetFilterClick -> {
                handleBottomSheetFilterClick(event.filter)
            }

            SearchUiEvent.OnApplyFilters -> {
                applyBottomSheetFilters()
            }

            SearchUiEvent.OnClearFilters -> {
                handleClearFilters()
            }

            is SearchUiEvent.OnSortChanged -> {
                val bsState = _uiState.value.sortBottomSheetState
                val newSortingList = bsState.sorting.map {
                    if (it.sortType == event.sort.sortType) {
                        if (it.selected) {
                            // Toggle direction if already selected
                            it.copy(descending = !it.descending)
                        } else {
                            // Select and default to descending for new selections
                            it.copy(selected = true, descending = true)
                        }
                    } else {
                        it.copy(selected = false)
                    }
                }

                _uiState.update {
                    it.copy(
                        sortBottomSheetState = bsState.copy(
                            isVisible = false,
                            sorting = newSortingList,
                            isSortActive = newSortingList.isSortActive()
                        )
                    )
                }
                val contentState = _uiState.value.contentState
                if (contentState is SearchContentState.Success) {
                    updateSearchContent(contentState, contentState.filters)
                }
            }

            SearchUiEvent.OnOpenSort -> {
                _uiState.update {
                    it.copy(
                        sortBottomSheetState = it.sortBottomSheetState.copy(
                            isVisible = true
                        )
                    )
                }
            }

            SearchUiEvent.OnDismissSort -> {
                _uiState.update {
                    it.copy(
                        sortBottomSheetState = it.sortBottomSheetState.copy(
                            isVisible = false
                        )
                    )
                }
            }

            SearchUiEvent.OnRefreshDiscover -> {
                refreshDiscoverFeed()
            }

            SearchUiEvent.OnDismissDiscoverRefresh -> {
                dismissDiscoverRefreshPrompt()
            }

            SearchUiEvent.OnRetrySearch -> {
                retrySearch()
            }

            SearchUiEvent.OnRetryDiscover -> {
                retryDiscoverFeed()
            }

            SearchUiEvent.OnClearActiveFilters -> {
                handleClearActiveFilters()
            }
        }
    }

    private fun handleClearFilters() {
        val bsState = _uiState.value.filtersBottomSheetState
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val clearedFilters = bsState.filters.deselectAll()

        val matchCount = calculateMatchCount(contentState.allGames, clearedFilters)
        _uiState.update {
            it.copy(
                filtersBottomSheetState = bsState.copy(
                    filters = clearedFilters, matchCount = matchCount
                )
            )
        }
    }

    /**
     * Clears the filters currently applied to the visible results, not just the bottom sheet's draft
     * selection -- see [SearchUiEvent.OnClearActiveFilters].
     */
    private fun handleClearActiveFilters() {
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        updateSearchContent(contentState, contentState.filters.deselectAll())
    }

    private fun List<GameFilterUiModel>.deselectAll(): List<GameFilterUiModel> = map { filter ->
        when (filter) {
            is GameFilterUiModel.Platform -> filter.copy(selected = false)
            is GameFilterUiModel.Genre -> filter.copy(selected = false)
            is GameFilterUiModel.GameType -> filter.copy(selected = false)
        }
    }

    private fun handleFilterClick(eventFilter: GameFilterUiModel) {
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val newFilters = toggleFilterSelection(contentState.filters, eventFilter)
        updateSearchContent(contentState, newFilters)
    }

    /**
     * Looks in whichever source the id could have come from: the search results grid, or the Discover
     * feed cached by [observeDiscoverFeed]. The two content areas are mutually exclusive on screen, but
     * either one's cards can hand a gameId to [toggleSave]/[openListSelector], so both need checking
     * regardless of which is currently rendered.
     */
    private fun findGame(gameId: Int): Game? {
        val contentState = _uiState.value.contentState
        if (contentState is SearchContentState.Success) {
            contentState.allGames.find { it.id == gameId }?.let { return it }
        }

        val feed = discoverFeed ?: return null
        return feed.popular.find { it.id == gameId }
            ?: feed.upcoming.find { it.id == gameId }
            ?: feed.recommended.firstNotNullOfOrNull { shelf -> shelf.games.find { it.id == gameId } }
    }

    /**
     * No optimistic update: the toggle writes to Room, which [observeWishlistedGameIds] re-emits from,
     * and that is what actually patches the card. The confirmation snackbar does not wait for that
     * round trip either -- [wasSaved] is read from [wishlistedGameIds], the shared membership cache, so
     * it agrees with the toggle regardless of whether it came from the search grid or the Discover feed.
     */
    private fun toggleSave(gameId: Int) {
        val game = findGame(gameId) ?: return
        val wasSaved = gameId in wishlistedGameIds.value

        viewModelScope.launch { toggleWishlistUseCase(game) }

        val messageRes =
            if (wasSaved) Res.string.removed_from_wishlist else Res.string.added_to_wishlist
        _uiEffect.trySend(
            SearchUiEffect.ShowSnackbar(message = UiText.StringResource(messageRes, game.name))
        )
    }

    private fun openListSelector(gameId: Int) {
        val game = findGame(gameId) ?: return

        viewModelScope.launch {
            val assignments = getWishlistAssignmentsUseCase(game.id).first()
            _uiState.update {
                it.copy(
                    listSelectorState = ListSelectorState(
                        gameId = game.id,
                        gameName = game.name,
                        availableLists = assignments.map { assignment -> assignment.toSelectorItem() }
                    )
                )
            }
        }
    }

    private fun toggleListSelection(listId: Long) {
        _uiState.update { current ->
            val selectorState = current.listSelectorState ?: return@update current
            current.copy(
                listSelectorState = selectorState.copy(
                    availableLists = selectorState.availableLists.map {
                        if (it.id == listId) it.copy(isSelected = !it.isSelected) else it
                    }
                )
            )
        }
    }

    /**
     * No isSaved sync needed beyond what [observeWishlistedGameIds] already does: toggling the default
     * list's membership through [addGameToListUseCase]/[removeGameFromListUseCase] re-emits from that
     * collector the same way a card's own save button does.
     */
    private fun confirmListSelection() {
        val selectorState = _uiState.value.listSelectorState ?: return
        // A catalogue game may not be stored yet; the use case stores it so the list can show it.
        val game = findGame(selectorState.gameId)
        if (game == null) {
            _uiState.update { it.copy(listSelectorState = null) }
            return
        }

        viewModelScope.launch {
            val originalAssignments = getWishlistAssignmentsUseCase(selectorState.gameId).first()
            val initialSelectedIds =
                originalAssignments.filter { it.isAssigned }.map { it.list.id }.toSet()
            val finalSelectedIds =
                selectorState.availableLists.filter { it.isSelected }.map { it.id }.toSet()

            val toAdd = finalSelectedIds - initialSelectedIds
            val toRemove = initialSelectedIds - finalSelectedIds

            toAdd.forEach { listId -> addGameToListUseCase(game, listId) }
            toRemove.forEach { listId -> removeGameFromListUseCase(selectorState.gameId, listId) }

            _uiState.update { it.copy(listSelectorState = null) }
        }
    }

    private fun handleBottomSheetFilterClick(eventFilter: GameFilterUiModel) {
        val bsState = _uiState.value.filtersBottomSheetState
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val newFilters = toggleFilterSelection(bsState.filters, eventFilter)

        val matchCount = calculateMatchCount(contentState.allGames, newFilters)
        _uiState.update {
            it.copy(
                filtersBottomSheetState = bsState.copy(
                    filters = newFilters, matchCount = matchCount
                )
            )
        }
    }

    private fun toggleFilterSelection(
        filters: List<GameFilterUiModel>,
        eventFilter: GameFilterUiModel
    ): List<GameFilterUiModel> = filters.map { filter ->
        when (filter) {
            is GameFilterUiModel.Platform -> {
                if (eventFilter is GameFilterUiModel.Platform && filter.id == eventFilter.id) {
                    filter.copy(selected = !filter.selected)
                } else filter
            }

            is GameFilterUiModel.Genre -> {
                if (eventFilter is GameFilterUiModel.Genre && filter.id == eventFilter.id) {
                    filter.copy(selected = !filter.selected)
                } else filter
            }

            is GameFilterUiModel.GameType -> {
                if (eventFilter is GameFilterUiModel.GameType && filter.id == eventFilter.id) {
                    filter.copy(selected = !filter.selected)
                } else filter
            }
        }
    }

    private fun applyBottomSheetFilters() {
        val bsState = _uiState.value.filtersBottomSheetState
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        _uiState.update { it.copy(filtersBottomSheetState = bsState.copy(isVisible = false)) }
        updateSearchContent(contentState, bsState.filters)
    }

    private fun calculateMatchCount(
        allGames: List<Game>, filters: List<GameFilterUiModel>
    ): Int {
        return filterGames(allGames, filters).size
    }

    private fun filterGames(
        allGames: List<Game>, filters: List<GameFilterUiModel>
    ): List<Game> {
        val selectedPlatformIds =
            filters.filterIsInstance<GameFilterUiModel.Platform>().filter { it.selected }
                .map { it.id }
        val selectedGenreIds =
            filters.filterIsInstance<GameFilterUiModel.Genre>().filter { it.selected }.map { it.id }
        val selectedGameTypeIds =
            filters.filterIsInstance<GameFilterUiModel.GameType>().filter { it.selected }
                .map { it.id }

        return allGames.filter { game ->
            val gamePlatformIds = game.platforms.map { it.id }
            val matchesPlatform =
                selectedPlatformIds.isEmpty() || selectedPlatformIds.all { it in gamePlatformIds }
            val gameGenreIds = game.genres.map { it.id }
            val matchesGenre =
                selectedGenreIds.isEmpty() || selectedGenreIds.all { it in gameGenreIds }
            val matchesGameType =
                selectedGameTypeIds.isEmpty() || game.gameType.id in selectedGameTypeIds
            matchesPlatform && matchesGenre && matchesGameType
        }
    }

    /**
     * @param keepSourceOrder True for any browse, where "Relevance" means "Top picks": the order the games
     * arrived in -- the server's popularity ranking for a genre browse, the shelf's own order for a Discover
     * shelf grid. A query-text relevance score has nothing to say about a browse, and re-scoring it would
     * throw that order away. Descending keeps it, ascending reverses it; the other sorts are the same as for
     * a text search.
     */
    private fun sortGames(
        games: List<Game>,
        sortModel: SortingUiModel?,
        query: String = "",
        keepSourceOrder: Boolean = false
    ): List<Game> {
        val currentSort = sortModel ?: return games

        return when (currentSort.sortType) {
            SearchSort.RELEVANCE -> {
                if (keepSourceOrder) {
                    if (currentSort.descending) games else games.reversed()
                } else if (currentSort.descending) games.sortedByDescending {
                    calculateGameRelevanceScore(it, query)
                }
                else games.sortedBy { calculateGameRelevanceScore(it, query) }
            }

            SearchSort.NAME -> {
                if (currentSort.descending) games.sortedByDescending { it.name }
                else games.sortedBy { it.name }
            }

            SearchSort.RATING -> {
                if (currentSort.descending) {
                    games.sortedWith(
                        compareByDescending<Game> { it.getDisplayRating() }
                            .thenByDescending { it.ratingCount }
                    )
                } else {
                    games.sortedWith(
                        compareBy<Game> { it.getDisplayRating() }
                            .thenBy { it.ratingCount }
                    )
                }
            }

            SearchSort.RELEASE_DATE -> {
                if (currentSort.descending) games.sortedByDescending { it.releaseDate }
                else games.sortedBy { it.releaseDate }
            }
        }
    }

    private fun updateSearchContent(
        contentState: SearchContentState.Success, newFilters: List<GameFilterUiModel>
    ) {
        val query = textFieldState.text.toString()
        val filteredGames = filterGames(contentState.allGames, newFilters)
        val sortedGames = sortGames(
            filteredGames,
            _uiState.value.sortBottomSheetState.selectedSorting,
            query,
            keepSourceOrder = _uiState.value.activeBrowse != null
        )

        _uiState.update {
            it.copy(
                contentState = contentState.copy(
                    games = sortedGames.toGameItemList(wishlistedGameIds.value),
                    filters = newFilters
                )
            )
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        // Only one search may land its result: without this, a slow earlier request finishing after a
        // later one would overwrite it.
        searchJob?.cancel()
        // After the guard above: a blank submit during a genre browse must leave the browse as it was, or
        // its results would sit under the default placeholder. A submitted query replaces the browse.
        _uiState.update {
            it.copy(
                suggestions = SearchSuggestionsUiModel(),
                activeBrowse = null,
                sortBottomSheetState = it.sortBottomSheetState.withRelevanceLabel(isBrowse = false)
            )
        }
        suggestionsResetTrigger.tryEmit("")

        searchJob = viewModelScope.launch {
            addSearchToHistoryUseCase(query)

            _uiState.update { it.copy(contentState = SearchContentState.Loading) }
            searchGamesUseCase(query).onSuccess { searchResult ->
                val sortedGames = sortGames(
                    searchResult.games, _uiState.value.sortBottomSheetState.selectedSorting, query
                )

                val newState = if (sortedGames.isEmpty()) {
                    SearchContentState.Empty
                } else {
                    val filters =
                        searchResult.platforms.toPlatformFilters() + searchResult.genres.toGenreFilters() + getInitialGameTypeFilters()

                    SearchContentState.Success(
                        games = sortedGames.toGameItemList(wishlistedGameIds.value),
                        filters = filters,
                        allGames = sortedGames
                    )
                }
                _uiState.update { it.copy(contentState = newState) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        contentState = SearchContentState.Error(
                            message = error.toUiText()
                        )
                    )
                }
            }
        }
    }

    /**
     * Shows the popular, recent, released games of [genre] in the results grid, as a text search shows its
     * matches. Differs from [performSearch] in three ways: the text field is emptied (the bar's placeholder
     * names the genre instead), the games stay in the server's popularity order (see [sortGames]), and the
     * browse is never written to the search history -- it is not something the user typed.
     */
    private fun browseGenre(genre: BrowseGenreUiModel) {
        searchJob?.cancel()

        textFieldState.edit { replace(0, length, "") }
        _uiState.update {
            it.copy(
                suggestions = SearchSuggestionsUiModel(),
                activeBrowse = SearchBrowseUiModel.Genre(genre),
                contentState = SearchContentState.Loading,
                sortBottomSheetState = it.sortBottomSheetState.withRelevanceLabel(isBrowse = true)
            )
        }
        suggestionsResetTrigger.tryEmit("")

        searchJob = viewModelScope.launch {
            browseGenreUseCase(genre.id).onSuccess { searchResult ->
                val newState = if (searchResult.games.isEmpty()) {
                    SearchContentState.Empty
                } else {
                    val filters =
                        searchResult.platforms.toPlatformFilters() + searchResult.genres.toGenreFilters() + getInitialGameTypeFilters()
                    val sortedGames = sortGames(
                        searchResult.games,
                        _uiState.value.sortBottomSheetState.selectedSorting,
                        keepSourceOrder = true
                    )

                    SearchContentState.Success(
                        games = sortedGames.toGameItemList(wishlistedGameIds.value),
                        filters = filters,
                        // Server order, not the sorted one: re-sorting and filtering start from here, and
                        // "Top picks" is only recoverable from the original ranking.
                        allGames = searchResult.games
                    )
                }
                _uiState.update { it.copy(contentState = newState) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(contentState = SearchContentState.Error(message = error.toUiText()))
                }
            }
        }
    }

    /**
     * Shows every game of a Discover [shelf] in the results grid, in the shelf's own order.
     *
     * The grid is a snapshot of games the feed already holds, so unlike [browseGenre] and [performSearch]
     * this is synchronous: no request, no `Loading`, no `Error`, and the grid is there in the same frame as
     * the tap. A later feed refresh changes [SearchUiState.discover] but not what is open here.
     *
     * The shelf is looked up by its key, never its title, in the feed as it is *now*: a refresh between the
     * row being rendered and its arrow being tapped can drop or reorder a personalised shelf, and then the tap
     * does nothing rather than opening some other shelf under the title the user tapped.
     */
    private fun openShelf(shelf: DiscoverShelfUiModel) {
        // A double tap: the grid is already this shelf, and re-opening it would only reset its sort and filters.
        val open = _uiState.value.activeBrowse
        if (open is SearchBrowseUiModel.Shelf && open.key == shelf.key) return

        val games = discoverFeed?.shelfGames(shelf.key)?.takeIf { it.isNotEmpty() } ?: return

        // Cancelled before any state changes, and that is what keeps SearchUiState's invariant true: only a
        // search coroutine ever writes Loading, Empty or Error, so with none left running nothing can land
        // on top of the grid, and Retry (which exists only on Error) can never be shown for it.
        searchJob?.cancel()
        searchJob = null

        textFieldState.edit { replace(0, length, "") }

        // No excluded genre, unlike a genre browse: a shelf is not defined by one genre, so every genre its
        // games carry narrows something.
        val facets = games.toSearchResult()
        val filters = facets.platforms.toPlatformFilters() +
            facets.genres.toGenreFilters() +
            getInitialGameTypeFilters()

        _uiState.update {
            val sortSheet = it.sortBottomSheetState.withRelevanceLabel(isBrowse = true)
            // allGames keeps the shelf's own order: re-sorting and filtering start from it, and "Top picks"
            // is only recoverable from the original ranking.
            val sorted = sortGames(games, sortSheet.selectedSorting, keepSourceOrder = true)
            it.copy(
                suggestions = SearchSuggestionsUiModel(),
                activeBrowse = SearchBrowseUiModel.Shelf(shelf.key, shelf.title),
                sortBottomSheetState = sortSheet,
                contentState = SearchContentState.Success(
                    games = sorted.toGameItemList(wishlistedGameIds.value),
                    filters = filters,
                    allGames = games
                )
            )
        }
        suggestionsResetTrigger.tryEmit("")
    }

    /**
     * Re-runs what failed: the genre browse while one is active, otherwise the query still held by
     * [textFieldState]. [performSearch] re-adds that query to history through [addSearchToHistoryUseCase],
     * which is keyed by the query text, so this only refreshes its timestamp rather than creating a
     * duplicate entry. A genre browse never touches the history, retried or not.
     */
    private fun retrySearch() {
        when (val browse = _uiState.value.activeBrowse) {
            is SearchBrowseUiModel.Genre -> browseGenre(browse.genre)
            null -> performSearch(textFieldState.text.toString())
            // Nothing to retry: a shelf grid is only ever Success (see SearchUiState.activeBrowse), so no
            // Retry button can be on screen for it, and openShelf cancels every search that could have
            // written an Error before it sets the shelf.
            is SearchBrowseUiModel.Shelf -> Unit
        }
    }

    private fun initSearchHistory() {
        viewModelScope.launch {
            getRecentSearchActivityUseCase().collect { activity ->
                _uiState.update { currentState ->
                    currentState.copy(
                        history = SearchHistoryUiModel(
                            queries = activity.queries, games = activity.games.toGameItemList()
                        )
                    )
                }
            }
        }
    }

    /**
     * Keeps the "Browse by genre" row current for the whole life of the ViewModel. The state is seeded with
     * the five defaults, so the row is never missing while the first emission (one local read) is on its way;
     * after that it follows the user's library.
     */
    private fun observeBrowseGenres() {
        viewModelScope.launch {
            getBrowseGenresUseCase().collect { genres ->
                _uiState.update { it.copy(browseGenres = genres.toBrowseGenreUiModels()) }
            }
        }
    }

    /**
     * Collects the feed for the whole life of the ViewModel: the use case re-emits whenever the user
     * changes their platform selection or taps [refreshDiscoverFeed], so the feed stays current even
     * while search results hold the content area. It writes to its own slot unconditionally -- nothing
     * here needs to know what is on screen, because [SearchUiState.discover] is only rendered while the
     * results are [SearchContentState.Idle].
     *
     * Only the first fetch shows a spinner. A later one is triggered by the user having just changed a
     * setting or asked for a refresh, and leaving the previous feed up until the new one lands reads
     * better than blanking a screen they were already looking at.
     */
    private fun observeDiscoverFeed() {
        viewModelScope.launch {
            getDiscoverFeedUseCase(refresh = discoverRefresh).collect { result ->
                val newState = when (result) {
                    is AppResult.Success -> {
                        discoverFeed = result.data
                        result.data.toDiscoverContentState(wishlistedGameIds.value)
                    }

                    is AppResult.Failure -> DiscoverContentState.Error(result.error.toUiText())
                }
                _uiState.update { it.copy(discover = newState) }
            }
        }
    }

    /**
     * Patches both content areas' games with the ids currently in the default wishlist, in place --
     * filtering, sorting and the shelf layout are not re-run, only each card's saved flag changes.
     * Collected for the whole life of the ViewModel, the same way [observeDiscoverFeed] is, since a
     * toggle from either area's card has to reach every card showing that same game, on screen or not.
     */
    private fun observeWishlistedGameIds() {
        viewModelScope.launch {
            getWishlistedGameIdsUseCase().collect { ids ->
                wishlistedGameIds.value = ids

                _uiState.update { current ->
                    val contentState = current.contentState
                    val patchedContentState = if (contentState is SearchContentState.Success) {
                        contentState.copy(games = contentState.games.withSavedState(ids))
                    } else {
                        contentState
                    }

                    val discover = current.discover
                    val patchedDiscover = if (discover is DiscoverContentState.Content) {
                        discover.copy(
                            hero = discover.hero?.let { it.copy(isSaved = it.id in ids) },
                            popular = discover.popular?.withSavedState(ids),
                            upcoming = discover.upcoming?.withSavedState(ids),
                            recommended = discover.recommended.map { it.withSavedState(ids) }
                        )
                    } else {
                        discover
                    }

                    current.copy(contentState = patchedContentState, discover = patchedDiscover)
                }
            }
        }
    }

    private fun SortBottomSheetState.withRelevanceLabel(isBrowse: Boolean): SortBottomSheetState =
        copy(sorting = sorting.withRelevanceLabel(isBrowse))

    private fun List<GameItemUiModel>.withSavedState(ids: Set<Int>): List<GameItemUiModel> =
        map { it.copy(isSaved = it.id in ids) }

    private fun DiscoverShelfUiModel.withSavedState(ids: Set<Int>): DiscoverShelfUiModel =
        copy(games = games.withSavedState(ids))

    /**
     * Flags the current feed as reloading before the trigger even reaches the use case, so the prompt
     * shows a spinner immediately instead of sitting inert for the length of the network round trip.
     * Only meaningful while a feed is already on screen -- there is nothing to mark refreshing before
     * the first load, and [DiscoverContentState.Loading] already covers that case.
     */
    private fun refreshDiscoverFeed() {
        val content = _uiState.value.discover
        if (content !is DiscoverContentState.Content) return

        _uiState.update { it.copy(discover = content.copy(isRefreshing = true)) }
        discoverRefresh.tryEmit(Unit)
    }

    /**
     * Retries a failed feed load. Unlike [refreshDiscoverFeed], which only flags an already-loaded feed
     * as refreshing, this applies to [DiscoverContentState.Error] -- there is no content to keep on
     * screen while the retry runs, so the state goes back to [DiscoverContentState.Loading] instead.
     */
    private fun retryDiscoverFeed() {
        val content = _uiState.value.discover
        if (content !is DiscoverContentState.Error) return

        _uiState.update { it.copy(discover = DiscoverContentState.Loading) }
        discoverRefresh.tryEmit(Unit)
    }

    /**
     * Overwrites [DiscoverContentState.Content.isStale] locally rather than telling the use case
     * anything -- staleness is a fact about the data, dismissal is not. A later emission that is
     * genuinely stale again (a further genre shift, or a fresh fetch landing) is a different value from
     * the one this wrote, so it reaches the screen the same way any other content change would.
     */
    private fun dismissDiscoverRefreshPrompt() {
        val content = _uiState.value.discover
        if (content !is DiscoverContentState.Content) return

        _uiState.update { it.copy(discover = content.copy(isStale = false)) }
    }

    /**
     * Hands the content area back to the feed. Nothing is restored or re-fetched: the feed was never
     * interrupted by the search and has been sitting in its own slot the whole time.
     */
    private fun clearSearch() {
        searchJob?.cancel()
        searchJob = null

        textFieldState.edit { replace(0, length, "") }
        _uiState.update {
            it.copy(
                suggestions = SearchSuggestionsUiModel(),
                activeBrowse = null,
                contentState = SearchContentState.Idle,
                sortBottomSheetState = it.sortBottomSheetState.withRelevanceLabel(isBrowse = false)
            )
        }
    }

    private fun initSearchSuggestions() {
        viewModelScope.launch {
            merge(
                snapshotFlow { textFieldState.text.toString() }.distinctUntilChanged(),
                suggestionsResetTrigger
            ).collectLatest { query ->
                if (query.length < MIN_SUGGESTION_QUERY_LENGTH) {
                    _uiState.update { it.copy(suggestions = SearchSuggestionsUiModel()) }
                    return@collectLatest
                }

                // 1. Instant local history suggestions
                val local = getSearchSuggestionsUseCase.getLocalSuggestions(query)
                    .filterIsInstance<SearchSuggestion.HistorySuggestion>()
                    .map { it.query }

                _uiState.update {
                    it.copy(suggestions = it.suggestions.copy(historySuggestions = local))
                }

                // 2. Debounced remote fetch -- collectLatest cancels this automatically
                // as soon as the query changes again, replacing manual job tracking.
                delay(SUGGESTIONS_DEBOUNCE)

                // Only now is there a request to wait for: flagging it before the delay made the
                // placeholder rows flash on every keystroke.
                _uiState.update {
                    it.copy(suggestions = it.suggestions.copy(isLoadingRemote = true))
                }

                val games = getSearchSuggestionsUseCase.getRemoteSuggestions(query)
                    .filterIsInstance<SearchSuggestion.GameSuggestion>()
                    .map { it.game }
                    .toSuggestionUiModels()
                _uiState.update {
                    it.copy(
                        suggestions = it.suggestions.copy(
                            gameSuggestions = games,
                            isLoadingRemote = false
                        )
                    )
                }
            }
        }
    }
}
