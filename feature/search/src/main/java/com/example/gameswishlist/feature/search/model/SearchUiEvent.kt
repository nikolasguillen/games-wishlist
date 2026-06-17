package com.example.gameswishlist.feature.search.model

sealed interface SearchUiEvent {
    data class OnQueryChange(val query: String) : SearchUiEvent
    data object OnSearchTriggered : SearchUiEvent
    data object OnClearQuery : SearchUiEvent
}