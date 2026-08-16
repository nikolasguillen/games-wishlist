package com.example.gameswishlist.feature.lists.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ListsUiState(
    val contentState: ListsContentState = ListsContentState.Loading
)
