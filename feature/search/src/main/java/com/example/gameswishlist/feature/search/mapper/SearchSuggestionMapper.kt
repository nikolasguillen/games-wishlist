package com.example.gameswishlist.feature.search.mapper

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.feature.search.model.GameSuggestionUiModel

/**
 * Maps a domain [Game] to a [GameSuggestionUiModel].
 * Formats the developer/publisher and release year into a subtitle.
 */
internal fun Game.toSuggestionUiModel(): GameSuggestionUiModel {
    val developer = developers.firstOrNull()?.name
    val publisher = publishers.firstOrNull()?.name
    val year = releaseDate?.take(4) ?: ""

    val subtitle = buildString {
        val company = developer ?: publisher
        if (company != null) {
            append(company)
            if (year.isNotEmpty()) append(" · ")
        }
        append(year)
    }

    return GameSuggestionUiModel(
        id = id,
        name = name,
        coverUrl = backgroundImage,
        subtitle = subtitle
    )
}

internal fun List<Game>.toSuggestionUiModels(): List<GameSuggestionUiModel> {
    return map { it.toSuggestionUiModel() }
}
