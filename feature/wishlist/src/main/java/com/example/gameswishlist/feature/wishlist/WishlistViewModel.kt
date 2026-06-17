package com.example.gameswishlist.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gameswishlist.core.domain.usecase.list.GetGamesByListUseCase
import com.example.gameswishlist.core.ui.mapper.toGameItemList
import com.example.gameswishlist.core.ui.model.GameItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val getGamesByListUseCase: GetGamesByListUseCase
) : ViewModel() {

    private val _games = MutableStateFlow<List<GameItem>>(emptyList())
    val games: StateFlow<List<GameItem>> = _games.asStateFlow()

    fun loadGames(listId: Long) {
        viewModelScope.launch {
            getGamesByListUseCase(listId)
                .map { it.toGameItemList() }
                .collect { games ->
                    _games.value = games
                }
        }
    }
}
