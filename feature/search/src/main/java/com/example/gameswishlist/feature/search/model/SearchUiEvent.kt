package com.example.gameswishlist.feature.search.model

sealed interface SearchUiEvent {
    data class OnSearchTriggered(val query: String) : SearchUiEvent
    data object OnClearHistory : SearchUiEvent
    data class OnHistoryItemRemoved(val query: String) : SearchUiEvent
}