package com.example.gameswishlist.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.common.calculateGameRelevanceScore
import com.example.gameswishlist.core.domain.usecase.ToggleWishlistUseCase
import com.example.gameswishlist.core.domain.usecase.discover.GetDiscoverFeedUseCase
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistedGameIdsUseCase
import com.example.gameswishlist.core.domain.usecase.list.RemoveGameFromListUseCase
import com.example.gameswishlist.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearAllHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearRecentGamesUseCase
import com.example.gameswishlist.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetRecentSearchActivityUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.example.gameswishlist.core.domain.usecase.search.RemoveRecentGameUseCase
import com.example.gameswishlist.core.domain.usecase.search.SearchGamesUseCase
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.SearchSuggestion
import com.example.gameswishlist.core.ui.mapper.getDisplayRating
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.mapper.getInitialGameTypeFilters
import com.example.gameswishlist.feature.search.mapper.getInitialSortFilters
import com.example.gameswishlist.feature.search.mapper.isSortActive
import com.example.gameswishlist.feature.search.mapper.toDiscoverContentState
import com.example.gameswishlist.feature.search.mapper.toGenreFilters
import com.example.gameswishlist.feature.search.mapper.toPlatformFilters
import com.example.gameswishlist.feature.search.mapper.toSelectorItem
import com.example.gameswishlist.feature.search.mapper.toSuggestionUiModels
import com.example.gameswishlist.feature.search.model.DiscoverContentState
import com.example.gameswishlist.feature.search.model.FilterBottomSheetState
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.ListSelectorState
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchHistoryUiModel
import com.example.gameswishlist.feature.search.model.SearchSort
import com.example.gameswishlist.feature.search.model.SearchSuggestionsUiModel
import com.example.gameswishlist.feature.search.model.SearchUiEffect
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import com.example.gameswishlist.feature.search.model.SortBottomSheetState
import com.example.gameswishlist.feature.search.model.SortingUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private val SUGGESTIONS_DEBOUNCE = 300.milliseconds

/**
 * Two characters match too much to be useful: the remote suggestion query is a substring match on the
 * title, so "ze" returns noise for a request IGDB rate-limits on a shared credential.
 */
private const val MIN_SUGGESTION_QUERY_LENGTH = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchGamesUseCase: SearchGamesUseCase,
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

    init {
        initSearchHistory()
        initSearchSuggestions()
        observeDiscoverFeed()
        observeWishlistedGameIds()
    }

    internal fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnSearchTriggered -> {
                textFieldState.setTextAndPlaceCursorAtEnd(event.query)
                performSearch(query = event.query)
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
        }
    }

    private fun handleClearFilters() {
        val bsState = _uiState.value.filtersBottomSheetState
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val clearedFilters = bsState.filters.map { filter ->
            when (filter) {
                is GameFilterUiModel.Platform -> filter.copy(selected = false)
                is GameFilterUiModel.Genre -> filter.copy(selected = false)
                is GameFilterUiModel.GameType -> filter.copy(selected = false)
            }
        }

        val matchCount = calculateMatchCount(contentState.allGames, clearedFilters)
        _uiState.update {
            it.copy(
                filtersBottomSheetState = bsState.copy(
                    filters = clearedFilters, matchCount = matchCount
                )
            )
        }
    }

    private fun handleFilterClick(eventFilter: GameFilterUiModel) {
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val newFilters = toggleFilterSelection(contentState.filters, eventFilter)
        updateSearchContent(contentState, newFilters)
    }

    /**
     * No optimistic update: the toggle writes to Room, which [observeWishlistedGameIds] re-emits from,
     * and that is what actually patches the card. The confirmation snackbar does not wait for that
     * round trip either -- [wasSaved] is read from the card's own state, from before the toggle.
     */
    private fun toggleSave(gameId: Int) {
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val game = contentState.allGames.find { it.id == gameId } ?: return
        val wasSaved = contentState.games.find { it.id == gameId }?.isSaved == true

        viewModelScope.launch { toggleWishlistUseCase(game) }

        val messageRes = if (wasSaved) R.string.removed_from_wishlist else R.string.added_to_wishlist
        _uiEffect.trySend(
            SearchUiEffect.ShowSnackbar(message = UiText.StringResource(messageRes, game.name))
        )
    }

    private fun openListSelector(gameId: Int) {
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val game = contentState.allGames.find { it.id == gameId } ?: return

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

        viewModelScope.launch {
            val originalAssignments = getWishlistAssignmentsUseCase(selectorState.gameId).first()
            val initialSelectedIds = originalAssignments.filter { it.isAssigned }.map { it.list.id }.toSet()
            val finalSelectedIds = selectorState.availableLists.filter { it.isSelected }.map { it.id }.toSet()

            val toAdd = finalSelectedIds - initialSelectedIds
            val toRemove = initialSelectedIds - finalSelectedIds

            toAdd.forEach { listId -> addGameToListUseCase(selectorState.gameId, listId) }
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

    private fun sortGames(
        games: List<Game>,
        sortModel: SortingUiModel?,
        query: String = ""
    ): List<Game> {
        val currentSort = sortModel ?: return games

        return when (currentSort.sortType) {
            SearchSort.RELEVANCE -> {
                if (currentSort.descending) games.sortedByDescending {
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
        val sortedGames =
            sortGames(filteredGames, _uiState.value.sortBottomSheetState.selectedSorting, query)

        _uiState.update {
            it.copy(
                contentState = contentState.copy(
                    games = sortedGames.toGameItemList(wishlistedGameIds.value), filters = newFilters
                )
            )
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        _uiState.update { it.copy(suggestions = SearchSuggestionsUiModel()) }
        suggestionsResetTrigger.tryEmit("")

        viewModelScope.launch {
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
     * Re-runs the query still held by [textFieldState]. [performSearch] re-adds it to history through
     * [addSearchToHistoryUseCase], which is keyed by the query text, so this only refreshes its
     * timestamp rather than creating a duplicate entry.
     */
    private fun retrySearch() {
        performSearch(textFieldState.text.toString())
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
                    is AppResult.Success -> result.data.toDiscoverContentState()
                    is AppResult.Failure -> DiscoverContentState.Error(result.error.toUiText())
                }
                _uiState.update { it.copy(discover = newState) }
            }
        }
    }

    /**
     * Patches [SearchContentState.Success.games] with the games currently in the default wishlist,
     * in place -- filtering and sorting are not re-run, only each card's saved flag changes. Collected
     * for the whole life of the ViewModel, the same way [observeDiscoverFeed] is, since a toggle from
     * the card has to reach a search result that was already on screen before the toggle.
     */
    private fun observeWishlistedGameIds() {
        viewModelScope.launch {
            getWishlistedGameIdsUseCase().collect { ids ->
                wishlistedGameIds.value = ids

                _uiState.update { current ->
                    val contentState = current.contentState
                    if (contentState !is SearchContentState.Success) return@update current

                    current.copy(
                        contentState = contentState.copy(
                            games = contentState.games.map { it.copy(isSaved = it.id in ids) }
                        )
                    )
                }
            }
        }
    }

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
        textFieldState.edit { replace(0, length, "") }
        _uiState.update {
            it.copy(
                suggestions = SearchSuggestionsUiModel(),
                contentState = SearchContentState.Idle
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
