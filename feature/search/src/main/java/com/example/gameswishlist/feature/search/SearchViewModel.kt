package com.example.gameswishlist.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.SearchGamesUseCase
import com.example.gameswishlist.core.model.RepositoryError
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.model.GameItem
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

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            searchGamesUseCase(query)
                .onSuccess { games ->
                    _uiState.update {
                        it.copy(
                            games = games.toGameItemList(),
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.toSearchMessage()
                        )
                    }
                }
        }
    }

    fun onClearQuery() {
        _uiState.update { it.copy(query = "") }
    }
}

private fun RepositoryError.toSearchMessage(): String {
    return when (this) {
        RepositoryError.NoNetwork -> "No internet connection.\nCheck your network and try again."
        RepositoryError.RequestTimeout -> "The request took too long.\nPlease try again."
        is RepositoryError.Http -> "The server returned an error (${code}).\nPlease try again later."
        is RepositoryError.Unknown -> "Something went wrong while searching.\nPlease try again."
    }
}

data class SearchUiState(
    val query: String = "",
    val games: List<GameItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
