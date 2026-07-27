package com.example.gameswishlist.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.DeleteListUseCase
import com.example.gameswishlist.core.domain.usecase.list.GetGamesByListUseCase
import com.example.gameswishlist.core.model.WishlistConstants
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = WishlistViewModel.Factory::class)
class WishlistViewModel @AssistedInject constructor(
    @Assisted private val listId: Long,
    private val getGamesByListUseCase: GetGamesByListUseCase,
    private val deleteListUseCase: DeleteListUseCase,
) : ViewModel() {

    private val _games = MutableStateFlow<List<GameItemUiModel>>(emptyList())
    val games: StateFlow<List<GameItemUiModel>> = _games.asStateFlow()

    /** The built-in wishlist is permanent, so the screen hides the action for it. */
    val canDeleteList: Boolean = listId != WishlistConstants.DEFAULT_WISHLIST_ID

    private val _listDeleted = Channel<Unit>(Channel.BUFFERED)
    val listDeleted = _listDeleted.receiveAsFlow()

    init {
        loadGames(listId)
    }

    fun deleteList() {
        viewModelScope.launch {
            if (deleteListUseCase(listId)) {
                _listDeleted.send(Unit)
            }
        }
    }

    private fun loadGames(listId: Long) {
        viewModelScope.launch {
            getGamesByListUseCase(listId)
                .map { it.toGameItemList() }
                .collect { games ->
                    _games.value = games
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(listId: Long): WishlistViewModel
    }
}
