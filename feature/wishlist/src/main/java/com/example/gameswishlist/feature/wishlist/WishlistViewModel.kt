package com.example.gameswishlist.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.GetGamesByListUseCase
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = WishlistViewModel.Factory::class)
class WishlistViewModel @AssistedInject constructor(
    @Assisted listId: Long,
    private val getGamesByListUseCase: GetGamesByListUseCase,
) : ViewModel() {

    private val _games = MutableStateFlow<List<GameItemUiModel>>(emptyList())
    val games: StateFlow<List<GameItemUiModel>> = _games.asStateFlow()

    init {
        loadGames(listId)
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
