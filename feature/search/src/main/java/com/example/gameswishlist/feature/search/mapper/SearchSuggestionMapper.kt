package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.model.SearchSuggestion
import com.example.gameswishlist.feature.search.model.SearchSuggestionUiModel

/**
 * Maps domain [SearchSuggestion] to [SearchSuggestionUiModel].
 * Handles formatting of subtitles for game suggestions.
 */
fun SearchSuggestion.toUiModel(): SearchSuggestionUiModel {
    return when (this) {
        is SearchSuggestion.HistorySuggestion -> {
            SearchSuggestionUiModel.History(query)
        }
        is SearchSuggestion.GameSuggestion -> {
            val developer = game.developers.firstOrNull()?.name
            val publisher = game.publishers.firstOrNull()?.name
            val year = game.releaseDate?.take(4) ?: ""
            
            val subtitle = buildString {
                val company = developer ?: publisher
                if (company != null) {
                    append(company)
                    if (year.isNotEmpty()) append(" · ")
                }
                append(year)
            }
            
            SearchSuggestionUiModel.Game(
                id = game.id,
                name = game.name,
                coverUrl = game.backgroundImage,
                subtitle = subtitle
            )
        }
    }
}

fun List<SearchSuggestion>.toUiModels(): List<SearchSuggestionUiModel> {
    return map { it.toUiModel() }
}
