package com.example.gameswishlist.feature.gamedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.GetGameDetailUseCase
import com.example.gameswishlist.core.domain.usecase.ToggleWishlistUseCase
import com.example.gameswishlist.core.domain.usecase.UpdateGameUseCase
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.example.gameswishlist.core.domain.usecase.list.RemoveGameFromListUseCase
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.WishlistConstants
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.mapper.toUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEffect
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState
import com.example.gameswishlist.feature.gamedetail.model.WishlistSelectorState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel(assistedFactory = GameDetailViewModel.Factory::class)
class GameDetailViewModel @AssistedInject constructor(
    @Assisted gameId: Int,
    private val getGameDetailUseCase: GetGameDetailUseCase,
    private val updateGameUseCase: UpdateGameUseCase,
    private val toggleWishlistUseCase: ToggleWishlistUseCase,
    private val getWishlistAssignmentsUseCase: GetWishlistAssignmentsUseCase,
    private val addGameToListUseCase: AddGameToListUseCase,
    private val removeGameFromListUseCase: RemoveGameFromListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameDetailUiState())
    internal val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<GameDetailUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    private var currentGame: Game? = null

    private val notesUpdateTrigger = Channel<Unit>(Channel.CONFLATED)

    init {
        loadGame(gameId)
        observeNotesUpdates()
    }

    @OptIn(FlowPreview::class)
    private fun observeNotesUpdates() {
        viewModelScope.launch {
            notesUpdateTrigger.receiveAsFlow().debounce(NOTES_SAVE_DEBOUNCE).collect {
                currentGame?.let { game -> updateGameUseCase(game) }
            }
        }
    }

    internal fun onEvent(event: GameDetailUiEvent) {
        when (event) {
            is GameDetailUiEvent.LoadGame -> loadGame(event.id)
            is GameDetailUiEvent.UpdateNotes -> updateNotes(event.notes)
            is GameDetailUiEvent.UpdatePriority -> updatePriority(event.priorityId)
            is GameDetailUiEvent.UpdateStatus -> updateStatus(event.statusId)
            is GameDetailUiEvent.ToggleGameInList -> toggleGameInList(event.listId)
            GameDetailUiEvent.ConfirmListSelection -> confirmListSelection()
            GameDetailUiEvent.OpenListSelector -> openListSelector()
            GameDetailUiEvent.DismissListSelector -> _uiState.update { it.copy(wishlistSelectorState = null) }
            GameDetailUiEvent.ToggleFavorite -> toggleFavorite()
            GameDetailUiEvent.ShareGame -> shareGame()
            is GameDetailUiEvent.NavigateToGame ->
                _uiEffect.trySend(GameDetailUiEffect.NavigateToGame(event.id))
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
            notesUpdateTrigger.trySend(Unit)
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

    private fun openListSelector() {
        val successState = uiState.value.contentState as? GameDetailContentState.Success ?: return
        val gameId = successState.game.id
        val gameName = successState.game.name
        
        viewModelScope.launch {
            val assignments = getWishlistAssignmentsUseCase(gameId).first()
            val uiAssignments = assignments.map { it.toUiModel() }

            _uiState.update { state ->
                state.copy(
                    wishlistSelectorState = WishlistSelectorState(
                        gameName = gameName,
                        availableLists = uiAssignments
                    )
                )
            }
        }
    }

    private fun toggleGameInList(listId: Long) {
        _uiState.update { state ->
            state.wishlistSelectorState?.let { selectorState ->
                val updatedLists = selectorState.availableLists.map { 
                    if (it.id == listId) it.copy(isSelected = !it.isSelected) else it
                }
                state.copy(
                    wishlistSelectorState = selectorState.copy(availableLists = updatedLists)
                )
            } ?: state
        }
    }

    private fun confirmListSelection() {
        val selectorState = _uiState.value.wishlistSelectorState ?: return
        val gameId = (uiState.value.contentState as? GameDetailContentState.Success)?.game?.id ?: return
        
        viewModelScope.launch {
            // Get original state to calculate delta
            val originalAssignments = getWishlistAssignmentsUseCase(gameId).first()
            val initialSelectedIds = originalAssignments.filter { it.isAssigned }.map { it.list.id }.toSet()
            val finalSelectedIds = selectorState.availableLists.filter { it.isSelected }.map { it.id }.toSet()

            val toAdd = finalSelectedIds - initialSelectedIds
            val toRemove = initialSelectedIds - finalSelectedIds

            toAdd.forEach { listId ->
                addGameToListUseCase(gameId, listId)
            }
            toRemove.forEach { listId ->
                removeGameFromListUseCase(gameId, listId)
            }

            // Sync isWishlisted flag if the default wishlist status changed
            val isDefaultWishlisted = finalSelectedIds.contains(WishlistConstants.DEFAULT_WISHLIST_ID)
            currentGame?.let { game ->
                if (game.isWishlisted != isDefaultWishlisted) {
                    val updatedGame = game.copy(isWishlisted = isDefaultWishlisted)
                    currentGame = updatedGame
                    updateContentState(updatedGame)
                }
            }

            _uiState.update { it.copy(wishlistSelectorState = null) }
        }
    }

    private fun toggleFavorite() {
        currentGame?.let { game ->
            viewModelScope.launch {
                toggleWishlistUseCase(game)
                val updatedGame = game.copy(isWishlisted = !game.isWishlisted)
                currentGame = updatedGame
                updateContentState(updatedGame)
            }
        }
    }

    private fun shareGame() {
        currentGame?.let { game ->
            val message = game.url?.let {
                UiText.StringResource(
                    R.string.share_game_with_url_message,
                    game.name,
                    it
                )
            } ?: run {
                UiText.StringResource(
                    R.string.share_game_message,
                    game.name
                )
            }
            _uiEffect.trySend(GameDetailUiEffect.ShareGame(message))
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

    private companion object {
        val NOTES_SAVE_DEBOUNCE = 500.milliseconds
    }
}
