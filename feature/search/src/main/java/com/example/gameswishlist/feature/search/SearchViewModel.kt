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
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        viewModelScope.launch {
            addSearchToHistoryUseCase(query)

            _uiState.update { it.copy(contentState = SearchContentState.Loading) }
            searchGamesUseCase(query)
                .onSuccess { games ->
                    val newState = if (games.isEmpty()) {
                        SearchContentState.Empty
                    } else {
                        SearchContentState.Success(games.toGameItemList())
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