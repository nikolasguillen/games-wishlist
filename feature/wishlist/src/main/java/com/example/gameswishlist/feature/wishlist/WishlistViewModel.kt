package com.example.gameswishlist.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.AddGameToListUseCase
import com.example.gameswishlist.core.domain.usecase.list.DeleteListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistDetailUseCase
import com.example.gameswishlist.core.domain.usecase.list.RemoveGameFromListUseCase
import com.example.gameswishlist.core.model.WishlistConstants
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.wishlist.mapper.toWishlistSectionUiModel
import com.example.gameswishlist.feature.wishlist.model.WishlistUiEffect
import com.example.gameswishlist.feature.wishlist.model.WishlistUiEvent
import com.example.gameswishlist.feature.wishlist.model.WishlistUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = WishlistViewModel.Factory::class)
class WishlistViewModel @AssistedInject constructor(
    @Assisted private val listId: Long,
    getWishlistDetailUseCase: GetWishlistDetailUseCase,
    private val deleteListUseCase: DeleteListUseCase,
    private val removeGameFromListUseCase: RemoveGameFromListUseCase,
    private val addGameToListUseCase: AddGameToListUseCase,
) : ViewModel() {
    private val _uiEffect = Channel<WishlistUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    private val canDeleteList = listId != WishlistConstants.DEFAULT_WISHLIST_ID

    // Single source of truth: reactively observes local storage, no manually mirrored copy.
    internal val uiState: StateFlow<WishlistUiState> = getWishlistDetailUseCase(listId)
        .onEach { detail ->
            // The list was deleted from under this screen (e.g. from another screen).
            if (detail == null) _uiEffect.send(WishlistUiEffect.NavigateBack)
        }
        .map { detail ->
            if (detail == null) {
                WishlistUiState()
            } else {
                WishlistUiState(
                    listName = UiText.DynamicString(detail.list.name),
                    sections = detail.games.toWishlistSectionUiModel(),
                    canDeleteList = canDeleteList
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WishlistUiState())

    internal fun onEvent(event: WishlistUiEvent) {
        return when (event) {
            is WishlistUiEvent.OnWishlistDeleted -> deleteList()
            is WishlistUiEvent.OnGameRemoved -> removeGame(event.gameId)
            is WishlistUiEvent.OnGameRemoveUndo -> undoRemoveGame(event.gameId)
        }
    }

    private fun deleteList() {
        viewModelScope.launch {
            if (deleteListUseCase(listId)) {
                _uiEffect.send(WishlistUiEffect.NavigateBack)
            } else {
                _uiEffect.send(
                    WishlistUiEffect.ShowSnackbar(
                        message = UiText.StringResource(R.string.unable_to_delete_wishlist)
                    )
                )
            }
        }
    }

    private fun removeGame(gameId: Int) {
        viewModelScope.launch {
            removeGameFromListUseCase(gameId, listId)
            _uiEffect.send(
                WishlistUiEffect.ShowSnackbar(
                    message = UiText.StringResource(R.string.game_removed_from_wishlist),
                    actionLabel = UiText.StringResource(R.string.undo_action),
                    actionEvent = WishlistUiEvent.OnGameRemoveUndo(gameId)
                )
            )
        }
    }

    private fun undoRemoveGame(gameId: Int) {
        viewModelScope.launch {
            addGameToListUseCase(gameId, listId)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(listId: Long): WishlistViewModel
    }
}
