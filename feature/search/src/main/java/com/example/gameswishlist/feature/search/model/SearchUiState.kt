package com.example.gameswishlist.feature.search.model

data class SearchUiState(
    val query: String = "test",
    val contentState: SearchContentState = SearchContentState.Initial
)