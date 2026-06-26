package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.model.WishlistList

data class GameDetailUiState(
    val contentState: GameDetailContentState = GameDetailContentState.Loading,
    val availableLists: List<WishlistList> = emptyList(),
    val isListSelectorVisible: Boolean = false
)