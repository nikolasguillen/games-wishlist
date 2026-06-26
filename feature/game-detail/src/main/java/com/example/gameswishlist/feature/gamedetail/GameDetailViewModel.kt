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
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.feature.gamedetail.mapper.toUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = GameDetailViewModel.Factory::class)
class GameDetailViewModel @AssistedInject constructor(
    @Assisted gameId: Int,
    private val getGameDetailUseCase: GetGameDetailUseCase,
    private val updateGameUseCase: UpdateGameUseCase,
    private val getListsUseCase: GetListsUseCase,
    private val addGameToListUseCase: AddGameToListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDetailUiState())
    val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private var currentGame: Game? = null

    init {
        viewModelScope.launch {
            getListsUseCase().collect { lists ->
                _uiState.update { it.copy(availableLists = lists) }
            }
        }

        loadGame(gameId)
    }

    fun onEvent(event: GameDetailUiEvent) {
        when (event) {
            is GameDetailUiEvent.LoadGame -> loadGame(event.id)
            is GameDetailUiEvent.UpdateNotes -> updateNotes(event.notes)
            is GameDetailUiEvent.UpdatePriority -> updatePriority(event.priorityId)
            is GameDetailUiEvent.UpdateStatus -> updateStatus(event.statusId)
            is GameDetailUiEvent.AddGameToList -> addGameToList(event.listId)
            GameDetailUiEvent.OpenListSelector -> _uiState.update { it.copy(isListSelectorVisible = true) }
            GameDetailUiEvent.DismissListSelector -> _uiState.update { it.copy(isListSelectorVisible = false) }
        }
    }

    private fun loadGame(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(contentState = GameDetailContentState.Loading) }
            getGameDetailUseCase(id)
                .onSuccess { game ->
                    currentGame = game
                    _uiState.update { it.copy(contentState = GameDetailContentState.Success(game.toUiModel())) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            contentState = GameDetailContentState.Error(
                                error.toUiText()
                            )
                        )
                    }
                }
        }
    }

    private fun updateNotes(notes: String) {
        currentGame?.let { game ->
            val updatedGame = game.copy(notes = notes)
            currentGame = updatedGame
            updateContentState(updatedGame)
            viewModelScope.launch {
                updateGameUseCase(updatedGame)
            }
        }
    }

    private fun updatePriority(priorityId: Int) {
        val priority = Priority.fromId(priorityId)
        currentGame?.let { game ->
            val newPriority = if (game.priority == priority) null else priority
            val updatedGame = game.copy(priority = newPriority)
            currentGame = updatedGame
            updateContentState(updatedGame)
            viewModelScope.launch {
                updateGameUseCase(updatedGame)
            }
        }
    }

    private fun updateStatus(statusId: Int) {
        val status = GameStatus.fromId(statusId)
        currentGame?.let { game ->
            val newStatus = if (game.status == status) null else status
            val updatedGame = game.copy(status = newStatus)
            currentGame = updatedGame
            updateContentState(updatedGame)
            viewModelScope.launch {
                updateGameUseCase(updatedGame)
            }
        }
    }

    private fun addGameToList(listId: Long) {
        currentGame?.let { game ->
            viewModelScope.launch {
                val updatedGame = game.copy(status = GameStatus.WANT_TO_BUY)
                updateGameUseCase(updatedGame)
                addGameToListUseCase(game.id, listId)

                // Update local state to reflect status change
                currentGame = updatedGame
                _uiState.update {
                    it.copy(
                        contentState = GameDetailContentState.Success(updatedGame.toUiModel()),
                        isListSelectorVisible = false
                    )
                }
            }
        }
    }

    private fun updateContentState(game: Game) {
        _uiState.update {
            it.copy(contentState = GameDetailContentState.Success(game.toUiModel()))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(gameId: Int): GameDetailViewModel
    }
}
