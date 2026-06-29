package com.example.gameswishlist.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.common.calculateGameRelevanceScore
import com.example.gameswishlist.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearAllHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearRecentGamesUseCase
import com.example.gameswishlist.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetRecentSearchActivityUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetSearchSuggestionsUseCase
import com.example.gameswishlist.core.domain.usecase.search.RemoveRecentGameUseCase
import com.example.gameswishlist.core.domain.usecase.search.SearchGamesUseCase
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.feature.search.mapper.getInitialGameTypeFilters
import com.example.gameswishlist.feature.search.mapper.getInitialSortFilters
import com.example.gameswishlist.feature.search.mapper.isSortActive
import com.example.gameswishlist.feature.search.mapper.toGenreFilters
import com.example.gameswishlist.feature.search.mapper.toPlatformFilters
import com.example.gameswishlist.feature.search.model.FilterBottomSheetState
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchHistoryUiModel
import com.example.gameswishlist.feature.search.model.SearchSort
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import com.example.gameswishlist.feature.search.model.SortBottomSheetState
import com.example.gameswishlist.feature.search.model.SortingUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchGamesUseCase: SearchGamesUseCase,
    private val addSearchToHistoryUseCase: AddSearchToHistoryUseCase,
    private val getRecentSearchActivityUseCase: GetRecentSearchActivityUseCase,
    private val deleteSearchHistoryItemUseCase: DeleteSearchHistoryItemUseCase,
    private val clearAllHistoryUseCase: ClearAllHistoryUseCase,
    private val removeRecentGameUseCase: RemoveRecentGameUseCase,
    private val clearRecentGamesUseCase: ClearRecentGamesUseCase,
    private val getSearchSuggestionsUseCase: GetSearchSuggestionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(
            sortBottomSheetState = SortBottomSheetState(
                sorting = getInitialSortFilters(),
                isSortActive = false // Initial is always Relevance/Desc
            )
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val textFieldState = TextFieldState()

    init {
        initSearchHistory()
        initSearchSuggestions()
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnSearchTriggered -> {
                textFieldState.setTextAndPlaceCursorAtEnd(event.query)
                performSearch(query = event.query)
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

        val newFilters = contentState.filters.map { filter ->
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
        updateSearchContent(contentState, newFilters)
    }

    private fun handleBottomSheetFilterClick(eventFilter: GameFilterUiModel) {
        val bsState = _uiState.value.filtersBottomSheetState
        val contentState = _uiState.value.contentState
        if (contentState !is SearchContentState.Success) return

        val newFilters = bsState.filters.map { filter ->
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

        val matchCount = calculateMatchCount(contentState.allGames, newFilters)
        _uiState.update {
            it.copy(
                filtersBottomSheetState = bsState.copy(
                    filters = newFilters, matchCount = matchCount
                )
            )
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
        allGames: List<com.example.gameswishlist.core.model.Game>, filters: List<GameFilterUiModel>
    ): Int {
        return filterGames(allGames, filters).size
    }

    private fun filterGames(
        allGames: List<com.example.gameswishlist.core.model.Game>, filters: List<GameFilterUiModel>
    ): List<com.example.gameswishlist.core.model.Game> {
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
        games: List<com.example.gameswishlist.core.model.Game>, sortModel: SortingUiModel?
    ): List<com.example.gameswishlist.core.model.Game> {
        val currentSort = sortModel ?: return games

        return when (currentSort.sortType) {
            SearchSort.RELEVANCE -> {
                if (currentSort.descending) games.sortedByDescending {
                    calculateGameRelevanceScore(
                        it
                    )
                }
                else games.sortedBy { calculateGameRelevanceScore(it) }
            }

            SearchSort.NAME -> {
                if (currentSort.descending) games.sortedByDescending { it.name }
                else games.sortedBy { it.name }
            }

            SearchSort.RATING -> {
                if (currentSort.descending) games.sortedByDescending { it.rating }
                else games.sortedBy { it.rating }
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
        val filteredGames = filterGames(contentState.allGames, newFilters)
        val sortedGames =
            sortGames(filteredGames, _uiState.value.sortBottomSheetState.selectedSorting)

        _uiState.update {
            it.copy(
                contentState = contentState.copy(
                    games = sortedGames.toGameItemList(), filters = newFilters
                )
            )
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            addSearchToHistoryUseCase(query)

            _uiState.update { it.copy(contentState = SearchContentState.Loading) }
            searchGamesUseCase(query).onSuccess { searchResult ->
                val sortedGames = sortGames(
                    searchResult.games, _uiState.value.sortBottomSheetState.selectedSorting
                )

                val newState = if (sortedGames.isEmpty()) {
                    SearchContentState.Empty
                } else {
                    val filters =
                        searchResult.platforms.toPlatformFilters() + searchResult.genres.toGenreFilters() + getInitialGameTypeFilters()

                    SearchContentState.Success(
                        games = sortedGames.toGameItemList(),
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

    private fun initSearchSuggestions() {
        snapshotFlow { textFieldState.text }
            .map { it.toString() }
            .distinctUntilChanged()
            .onEach { query ->
                if (query.length < 2) {
                    _uiState.update { it.copy(suggestions = emptyList()) }
                    return@onEach
                }
                _uiState.update { it.copy(suggestions = getSearchSuggestionsUseCase(query)) }
            }
            .launchIn(viewModelScope)
    }
}