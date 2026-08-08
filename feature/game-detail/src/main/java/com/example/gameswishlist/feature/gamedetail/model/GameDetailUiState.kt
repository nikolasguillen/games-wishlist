package com.example.gameswishlist.feature.gamedetail.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class GameDetailUiState(
    val contentState: GameDetailContentState = GameDetailContentState.Loading,
    val wishlistSelectorState: WishlistSelectorState? = null
)
