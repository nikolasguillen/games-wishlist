package com.example.gameswishlist.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val searchGamesUseCase: SearchGamesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.OnQueryChange -> {
                _uiState.update { it.copy(query = event.query) }
            }

            SearchUiEvent.OnSearchTriggered -> {
                performSearch()
            }

            SearchUiEvent.OnClearQuery -> {
                _uiState.update {
                    it.copy(query = "", contentState = SearchContentState.Initial)
                }
            }
        }
    }

    private fun performSearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        viewModelScope.launch {
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