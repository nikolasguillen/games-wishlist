package com.example.gameswishlist.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.DeleteListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetGamesByListUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = WishlistViewModel.Factory::class)
class WishlistViewModel @AssistedInject constructor(
    @Assisted private val listId: Long,
    private val getGamesByListUseCase: GetGamesByListUseCase,
    private val deleteListUseCase: DeleteListUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WishlistUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiEffect = Channel<WishlistUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        loadGames(listId)
    }

    fun onEvent(event: WishlistUiEvent) {
        return when (event) {
            is WishlistUiEvent.OnWishlistDeleted -> deleteList()
        }
    }

    private fun loadGames(listId: Long) {
        viewModelScope.launch {
            getGamesByListUseCase(listId)
                .map { it.toWishlistSectionUiModel() }
                .collect { sections ->
                    _uiState.update {
                        WishlistUiState(
                            sections = sections,
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
