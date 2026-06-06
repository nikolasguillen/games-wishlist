package com.example.gameswishlist.feature.gamedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.GetGameDetailUseCase
import com.example.gameswishlist.core.domain.usecase.UpdateGameUseCase
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetListsUseCase
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.WishlistList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val getGameDetailUseCase: GetGameDetailUseCase,
    private val updateGameUseCase: UpdateGameUseCase,
    private val getListsUseCase: GetListsUseCase,
    private val addGameToListUseCase: AddGameToListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    val availableLists: StateFlow<List<WishlistList>> = getListsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadGame(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getGameDetailUseCase(id)
                .onSuccess { game ->
                    _uiState.update { it.copy(game = game, isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun updateNotes(notes: String) {
        val currentGame = _uiState.value.game ?: return
        val updatedGame = currentGame.copy(notes = notes)
        _uiState.update { it.copy(game = updatedGame) }
        viewModelScope.launch {
            updateGameUseCase(updatedGame)
        }
    }

    fun updatePriority(priority: Int) {
        val currentGame = _uiState.value.game ?: return
        val updatedGame = currentGame.copy(priority = priority)
        _uiState.update { it.copy(game = updatedGame) }
        viewModelScope.launch {
            updateGameUseCase(updatedGame)
        }
    }

    fun updateStatus(status: GameStatus) {
        val currentGame = _uiState.value.game ?: return
        val updatedGame = currentGame.copy(status = status)
        _uiState.update { it.copy(game = updatedGame) }
        viewModelScope.launch {
            updateGameUseCase(updatedGame)
        }
    }

    fun addGameToList(listId: Long) {
        val currentGame = _uiState.value.game ?: return
        viewModelScope.launch {
            addGameToListUseCase(currentGame.id, listId)
        }
    }
}

data class GameDetailUiState(
    val game: Game? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
