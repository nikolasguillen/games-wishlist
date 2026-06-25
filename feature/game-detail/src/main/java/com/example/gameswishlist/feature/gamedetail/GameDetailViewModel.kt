package com.example.gameswishlist.feature.gamedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.GetGameDetailUseCase
import com.example.gameswishlist.core.domain.usecase.UpdateGameUseCase
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetListsUseCase
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.toUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameDetailViewModel @Inject constructor(
    private val getGameDetailUseCase: GetGameDetailUseCase,
    private val updateGameUseCase: UpdateGameUseCase,
    getListsUseCase: GetListsUseCase,
    private val addGameToListUseCase: AddGameToListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<GameDetailUiState>(GameDetailUiState.Loading)
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private var currentGame: Game? = null

    val availableLists: StateFlow<List<WishlistList>> = getListsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadGame(id: Int) {
        viewModelScope.launch {
            _uiState.value = GameDetailUiState.Loading
            getGameDetailUseCase(id)
                .onSuccess { game ->
                    currentGame = game
                    _uiState.value = GameDetailUiState.Success(game.toUiModel())
                }
                .onFailure { error ->
                    _uiState.value = GameDetailUiState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun updateNotes(notes: String) {
        currentGame?.let { game ->
            val updatedGame = game.copy(notes = notes)
            currentGame = updatedGame
            _uiState.value = GameDetailUiState.Success(updatedGame.toUiModel())
            viewModelScope.launch {
                updateGameUseCase(updatedGame)
            }
        }
    }

    fun updatePriority(priority: Priority) {
        currentGame?.let { game ->
            val updatedGame = game.copy(priority = priority)
            currentGame = updatedGame
            _uiState.value = GameDetailUiState.Success(updatedGame.toUiModel())
            viewModelScope.launch {
                updateGameUseCase(updatedGame)
            }
        }
    }

    fun updateStatus(status: GameStatus) {
        currentGame?.let { game ->
            val updatedGame = game.copy(status = status)
            currentGame = updatedGame
            _uiState.value = GameDetailUiState.Success(updatedGame.toUiModel())
            viewModelScope.launch {
                updateGameUseCase(updatedGame)
            }
        }
    }

    fun addGameToList(listId: Long) {
        currentGame?.let { game ->
            viewModelScope.launch {
                val updatedGame = game.copy(status = GameStatus.WANT_TO_BUY)
                updateGameUseCase(updatedGame)
                addGameToListUseCase(game.id, listId)
                
                // Update local state to reflect status change
                currentGame = updatedGame
                _uiState.value = GameDetailUiState.Success(updatedGame.toUiModel())
            }
        }
    }
}

sealed interface GameDetailUiState {
    data object Loading : GameDetailUiState
    data class Success(val game: GameDetailUiModel) : GameDetailUiState
    data class Error(val message: String) : GameDetailUiState
}
