package com.example.gameswishlist.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.search.AddSearchToHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.ClearAllHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.DeleteSearchHistoryItemUseCase
import com.example.gameswishlist.core.domain.usecase.search.GetSearchHistoryUseCase
import com.example.gameswishlist.core.domain.usecase.search.SearchGamesUseCase
import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.mapper.toPlatformFilters
import com.example.gameswishlist.feature.search.model.GameFilterUiModel
import com.example.gameswishlist.feature.search.model.SearchContentState
import com.example.gameswishlist.feature.search.model.SearchUiEvent
import com.example.gameswishlist.feature.search.model.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchGamesUseCase: SearchGamesUseCase,
    private val addSearchToHistoryUseCase: AddSearchToHistoryUseCase,
    private val getSearchHistoryUseCase: GetSearchHistoryUseCase,
    private val deleteSearchHistoryItemUseCase: DeleteSearchHistoryItemUseCase,
    private val clearAllHistoryUseCase: ClearAllHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSearchHistoryUseCase().collect {
                _uiState.update { currentState ->
                    currentState.copy(recentSearches = it)
                }
            }
        }
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnSearchTriggered -> {
                performSearch(query = event.query)
            }

            SearchUiEvent.OnClearHistory -> {
                viewModelScope.launch {
                    clearAllHistoryUseCase()
                }
            }

            is SearchUiEvent.OnHistoryItemRemoved -> {
                viewModelScope.launch {
                    deleteSearchHistoryItemUseCase(event.query)
                }
            }

            is SearchUiEvent.OnFilterClick -> {
                val contentState = _uiState.value.contentState
                if (contentState !is SearchContentState.Success) return

                val newFilters = contentState.filters.map { filter ->
                    when (filter) {
                        is GameFilterUiModel.Platform -> {
                            if (event.filter is GameFilterUiModel.Platform && filter.id == event.filter.id) {
                                filter.copy(selected = !filter.selected)
                            } else filter
                        }

                        is GameFilterUiModel.Genre -> {
                            if (event.filter is GameFilterUiModel.Genre && filter.id == event.filter.id) {
                                filter.copy(selected = !filter.selected)
                            } else filter
                        }
                    }
                }

                val selectedPlatformIds = newFilters
                    .filterIsInstance<GameFilterUiModel.Platform>()
                    .filter { it.selected }
                    .map { it.id }

                val selectedGenreIds = newFilters
                    .filterIsInstance<GameFilterUiModel.Genre>()
                    .filter { it.selected }
                    .map { it.id }

                val filteredGames = contentState.allGames.filter { game ->
                    val gamePlatformIds = game.platforms.map { it.id }
                    val matchesPlatform = selectedPlatformIds.isEmpty() ||
                            selectedPlatformIds.all { it in gamePlatformIds }

                    val gameGenreIds = game.genres.map { it.id }
                    val matchesGenre = selectedGenreIds.isEmpty() ||
                            selectedGenreIds.all { it in gameGenreIds }

                    matchesPlatform && matchesGenre
                }

                _uiState.update {
                    it.copy(
                        contentState = contentState.copy(
                            games = filteredGames.toGameItemList(),
                            filters = newFilters
                        )
                    )
                }
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            addSearchToHistoryUseCase(query)

            _uiState.update { it.copy(contentState = SearchContentState.Loading) }
            searchGamesUseCase(query)
                .onSuccess { searchResult ->
                    val newState = if (searchResult.games.isEmpty()) {
                        SearchContentState.Empty
                    } else {
                        SearchContentState.Success(
                            games = searchResult.games.toGameItemList(),
                            filters = searchResult.platforms.toPlatformFilters(),
                            allGames = searchResult.games
                        )
                    }
                    _uiState.update { it.copy(contentState = newState) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            contentState = SearchContentState.Error(
                                message = error.toSearchMessage()
                            )
                        )
                    }
                }
        }
    }
}

private fun RepositoryError.toSearchMessage(): UiText {
    return when (this) {
        RepositoryError.NoNetwork -> UiText.StringResource(R.string.error_no_network)
        RepositoryError.RequestTimeout -> UiText.StringResource(R.string.error_request_timeout)
        is RepositoryError.Http -> UiText.StringResource(R.string.error_http, code)
        is RepositoryError.Unknown -> UiText.StringResource(R.string.error_unknown)
    }
}