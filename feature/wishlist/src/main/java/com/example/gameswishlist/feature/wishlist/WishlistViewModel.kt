package com.example.gameswishlist.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.DeleteListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetWishlistDetailUseCase
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = WishlistViewModel.Factory::class)
class WishlistViewModel @AssistedInject constructor(
    @Assisted private val listId: Long,
    private val getWishlistDetailUseCase: GetWishlistDetailUseCase,
    private val deleteListUseCase: DeleteListUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WishlistUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<WishlistUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        observeWishlistDetail(listId)
    }

    fun onEvent(event: WishlistUiEvent) {
        return when (event) {
            is WishlistUiEvent.OnWishlistDeleted -> deleteList()
        }
    }

    private fun observeWishlistDetail(listId: Long) {
        viewModelScope.launch {
            getWishlistDetailUseCase(listId).collect { detail ->
                if (detail == null) {
                    // The list was deleted from under this screen (e.g. from another screen).
                    // Edge case, should never happen, but you never know.
                    _uiEffect.send(WishlistUiEffect.NavigateBack)
                    return@collect
                }
                _uiState.update {
                    WishlistUiState(
                        listName = UiText.DynamicString(detail.list.name),
                        sections = detail.games.toWishlistSectionUiModel(),
                        canDeleteList = listId != WishlistConstants.DEFAULT_WISHLIST_ID
                    )
                }
            }
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

    @AssistedFactory
    interface Factory {
        fun create(listId: Long): WishlistViewModel
    }
}
