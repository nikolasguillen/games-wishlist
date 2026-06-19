package com.example.gameswishlist.feature.search.model

data class SearchUiState(
    val recentSearches: List<String> = emptyList(),
    val contentState: SearchContentState = SearchContentState.Initial
)