package com.example.gameswishlist.feature.gamedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.GetGameDetailUseCase
import com.example.gameswishlist.core.domain.usecase.RefreshGameDetailUseCase
import com.example.gameswishlist.core.domain.usecase.ToggleWishlistUseCase
import com.example.gameswishlist.core.domain.usecase.UpdateGameUseCase
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistAssignmentsUseCase
import com.example.gameswishlist.core.domain.usecase.list.RemoveGameFromListUseCase
import com.example.gameswishlist.core.domain.usecase.translation.GetTranslationModelStatusUseCase
import com.example.gameswishlist.core.domain.usecase.translation.ObserveDescriptionTranslationEnabledUseCase
import com.example.gameswishlist.core.domain.usecase.translation.TranslateGameDescriptionUseCase
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.TranslationModelStatus
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.mapper.toUiModel
import com.example.gameswishlist.feature.gamedetail.model.DescriptionTranslationState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailContentState
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEffect
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiEvent
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiState
import com.example.gameswishlist.feature.gamedetail.model.WishlistSelectorState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = GameDetailViewModel.Factory::class)
class GameDetailViewModel @AssistedInject constructor(
    @Assisted private val gameId: Int,
    getGameDetailUseCase: GetGameDetailUseCase,
    private val refreshGameDetailUseCase: RefreshGameDetailUseCase,
    private val updateGameUseCase: UpdateGameUseCase,
    private val toggleWishlistUseCase: ToggleWishlistUseCase,
    private val getWishlistAssignmentsUseCase: GetWishlistAssignmentsUseCase,
    private val addGameToListUseCase: AddGameToListUseCase,
    private val removeGameFromListUseCase: RemoveGameFromListUseCase,
    private val translateGameDescriptionUseCase: TranslateGameDescriptionUseCase,
    private val observeDescriptionTranslationEnabledUseCase: ObserveDescriptionTranslationEnabledUseCase,
    private val getTranslationModelStatusUseCase: GetTranslationModelStatusUseCase
) : ViewModel() {

    // Single source of truth: reactively observes local storage. Mutations write through the
    // use cases below and this flow picks the change back up -- no manually mirrored copy.
    private val currentGameFlow = getGameDetailUseCase(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Local only: refreshGame is the sole writer, and observeContentState is the sole reader.
    private val _refreshError = MutableStateFlow<UiText?>(null)

    private val _uiState = MutableStateFlow(GameDetailUiState())
    internal val uiState: StateFlow<GameDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<GameDetailUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    init {
        refreshGame(gameId)
        observeContentState()
        observeDescriptionTranslation()
    }

    /**
     * Success always wins over a stale error, and both are recomputed from scratch on every change of
     * either input -- there is no second write site that has to remember to clear the error once a game
     * arrives (e.g. from a retry, or any other write to this row). Collected for the whole life of the
     * ViewModel, the same way `SearchViewModel.observeDiscoverFeed` is: this is the only piece of
     * [uiState] derived from a continuously-observed source, and folding it into [_uiState] here keeps
     * that the only place the derivation happens.
     */
    private fun observeContentState() {
        viewModelScope.launch {
            combine(currentGameFlow, _refreshError) { game, error ->
                when {
                    game != null -> GameDetailContentState.Success(game.toUiModel())
                    error != null -> GameDetailContentState.Error(error)
                    else -> GameDetailContentState.Loading
                }
            }.collect { contentState ->
                _uiState.update { it.copy(contentState = contentState) }
            }
        }
    }

    /**
     * Keyed on the description, not the whole game: currentGameFlow re-emits on every notes, status and
     * priority edit too, and without this operator every keystroke in the notes field would re-run
     * inference.
     */
    private fun observeDescriptionTranslation() {
        viewModelScope.launch {
            currentGameFlow
                .distinctUntilChangedBy { it?.description }
                .collect { game -> updateDescriptionTranslation(game?.description) }
        }
    }

    private suspend fun updateDescriptionTranslation(description: String?) {
        if (description.isNullOrBlank()) {
            _uiState.update { it.copy(descriptionTranslation = DescriptionTranslationState.Off) }
            return
        }
        val isEnabled = observeDescriptionTranslationEnabledUseCase().first()
        if (!isEnabled || getTranslationModelStatusUseCase() != TranslationModelStatus.READY) {
            _uiState.update { it.copy(descriptionTranslation = DescriptionTranslationState.Off) }
            return
        }
        _uiState.update { it.copy(descriptionTranslation = DescriptionTranslationState.InProgress) }
        val translation = translateGameDescriptionUseCase(gameId, description)
            ?.let { DescriptionTranslationState.Ready(it) }
            ?: DescriptionTranslationState.Off
        _uiState.update { it.copy(descriptionTranslation = translation) }
    }

    internal fun onEvent(event: GameDetailUiEvent) {
        when (event) {
            GameDetailUiEvent.Retry -> refreshGame(gameId)
            is GameDetailUiEvent.UpdateNotes -> updateNotes(event.notes)
            is GameDetailUiEvent.UpdatePriority -> updatePriority(event.priorityId)
            is GameDetailUiEvent.UpdateStatus -> updateStatus(event.statusId)
            is GameDetailUiEvent.ToggleGameInList -> toggleGameInList(event.listId)
            GameDetailUiEvent.ConfirmListSelection -> confirmListSelection()
            GameDetailUiEvent.OpenListSelector -> openListSelector()
            GameDetailUiEvent.DismissListSelector ->
                _uiState.update { it.copy(wishlistSelectorState = null) }
            GameDetailUiEvent.ToggleFavorite -> toggleFavorite()
            GameDetailUiEvent.ShareGame -> shareGame()
            is GameDetailUiEvent.NavigateToGame ->
                _uiEffect.trySend(GameDetailUiEffect.NavigateToGame(event.id))
        }
    }

    private fun refreshGame(id: Int) {
        viewModelScope.launch {
            _refreshError.value = null
            refreshGameDetailUseCase(id).onFailure { error ->
                _refreshError.value = error.toUiText()
            }
        }
    }

    private fun updateNotes(notes: String) {
        val game = currentGameFlow.value ?: return
        viewModelScope.launch { updateGameUseCase(game.copy(notes = notes)) }
    }

    private fun updatePriority(priorityId: Int) {
        val priority = Priority.fromId(priorityId)
        val game = currentGameFlow.value ?: return
        val newPriority = if (game.priority == priority) null else priority
        viewModelScope.launch { updateGameUseCase(game.copy(priority = newPriority)) }
    }

    private fun updateStatus(statusId: Int) {
        val status = GameStatus.fromId(statusId)
        val game = currentGameFlow.value ?: return
        val newStatus = if (game.status == status) null else status
        viewModelScope.launch { updateGameUseCase(game.copy(status = newStatus)) }
    }

    private fun openListSelector() {
        val game = currentGameFlow.value ?: return
        viewModelScope.launch {
            val assignments = getWishlistAssignmentsUseCase(game.id).first()
            _uiState.update {
                it.copy(
                    wishlistSelectorState = WishlistSelectorState(
                        gameName = UiText.DynamicString(game.name),
                        availableLists = assignments.map { assignment -> assignment.toUiModel() }
                    )
                )
            }
        }
    }

    private fun toggleGameInList(listId: Long) {
        _uiState.update { state ->
            val selectorState = state.wishlistSelectorState ?: return@update state
            state.copy(
                wishlistSelectorState = selectorState.copy(
                    availableLists = selectorState.availableLists.map {
                        if (it.id == listId) it.copy(isSelected = !it.isSelected) else it
                    }
                )
            )
        }
    }

    private fun confirmListSelection() {
        val selectorState = _uiState.value.wishlistSelectorState ?: return
        val game = currentGameFlow.value ?: return

        viewModelScope.launch {
            val originalAssignments = getWishlistAssignmentsUseCase(game.id).first()
            val initialSelectedIds = originalAssignments.filter { it.isAssigned }.map { it.list.id }.toSet()
            val finalSelectedIds = selectorState.availableLists.filter { it.isSelected }.map { it.id }.toSet()

            val toAdd = finalSelectedIds - initialSelectedIds
            val toRemove = initialSelectedIds - finalSelectedIds

            toAdd.forEach { listId -> addGameToListUseCase(game.id, listId) }
            toRemove.forEach { listId -> removeGameFromListUseCase(game.id, listId) }

            // No manual isWishlisted sync needed: observeGameDetail recombines with the
            // default list's cross-ref rows, so toggling WishlistConstants.DEFAULT_WISHLIST_ID
            // above already flows back through currentGameFlow.
            _uiState.update { it.copy(wishlistSelectorState = null) }
        }
    }

    private fun toggleFavorite() {
        val game = currentGameFlow.value ?: return
        viewModelScope.launch { toggleWishlistUseCase(game) }
    }

    private fun shareGame() {
        val game = currentGameFlow.value ?: return
        val message = game.url?.let {
            UiText.StringResource(
                R.string.share_game_with_url_message,
                game.name,
                it
            )
        } ?: UiText.StringResource(
            R.string.share_game_message,
            game.name
        )
        _uiEffect.trySend(GameDetailUiEffect.ShareGame(message))
    }

    @AssistedFactory
    interface Factory {
        fun create(gameId: Int): GameDetailViewModel
    }
}
