package com.example.gameswishlist.feature.search.model

data class SearchUiState(
    val query: String = "",
    val contentState: SearchContentState = SearchContentState.Initial
)