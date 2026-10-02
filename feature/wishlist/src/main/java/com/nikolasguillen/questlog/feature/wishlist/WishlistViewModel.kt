package com.nikolasguillen.questlog.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikolasguillen.questlog.core.domain.usecase.list.DeleteListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistDetailUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.RemoveGameFromListUseCase
import com.nikolasguillen.questlog.core.domain.usecase.list.SetDefaultListUseCase
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.wishlist.mapper.toWishlistSectionUiModel
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistContentState
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEffect
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiEvent
import com.nikolasguillen.questlog.feature.wishlist.model.WishlistUiState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val setDefaultListUseCase: SetDefaultListUseCase,
) : ViewModel() {
    private val _uiEffect = Channel<WishlistUiEffect>(Channel.BUFFERED)
    internal val uiEffect = _uiEffect.receiveAsFlow()

    // Single source of truth: reactively observes local storage, no manually mirrored copy.
    internal val uiState: StateFlow<WishlistUiState> = getWishlistDetailUseCase(listId)
        // Deleting a list that holds games invalidates several queries, so the flow can report it gone more
        // than once. Only the first report may navigate; every other emission has to get through.
        .distinctUntilChanged { old, new -> old == null && new == null }
        .onEach { detail ->
            // The list is gone: deleted from this screen, or from under it. This is the only place that
            // leaves the screen after a delete, so one delete pops exactly one screen.
            if (detail == null) _uiEffect.send(WishlistUiEffect.NavigateBack)
        }
        .map { detail ->
            if (detail == null) {
                WishlistUiState()
            } else {
                val sections = detail.games.toWishlistSectionUiModel()
                WishlistUiState(
                    listName = UiText.DynamicString(detail.list.name),
                    isDefaultList = detail.isDefault,
                    showListOptions = !detail.isDefault,
                    contentState = if (sections.isEmpty()) {
                        WishlistContentState.Empty
                    } else {
                        WishlistContentState.Success(sections)
                    }
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WishlistUiState())

    internal fun onEvent(event: WishlistUiEvent) {
        return when (event) {
            is WishlistUiEvent.OnSetAsDefault -> setAsDefault()
            is WishlistUiEvent.OnWishlistDeleted -> deleteList()
            is WishlistUiEvent.OnGameRemoved -> removeGame(event.gameId)
        }
    }

    private fun setAsDefault() {
        if (uiState.value.isDefaultList) return
        viewModelScope.launch {
            setDefaultListUseCase(listId)
            _uiEffect.send(
                WishlistUiEffect.ShowSnackbar(
                    message = UiText.StringResource(R.string.default_list_set_message)
                )
            )
        }
    }

    private fun deleteList() {
        viewModelScope.launch {
            if (!deleteListUseCase(listId)) {
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
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(listId: Long): WishlistViewModel
    }
}
