package com.example.gameswishlist.feature.gamedetail.model

data class GameDetailUiState(
    val contentState: GameDetailContentState = GameDetailContentState.Loading,
    val availableLists: List<WishlistListUiModel> = emptyList(),
    val isListSelectorVisible: Boolean = false
)
