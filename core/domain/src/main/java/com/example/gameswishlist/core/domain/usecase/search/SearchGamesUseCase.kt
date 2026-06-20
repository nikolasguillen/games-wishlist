package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.AppResult
import com.example.gameswishlist.core.model.SearchResult
import javax.inject.Inject

class SearchGamesUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String): AppResult<SearchResult> {
        return repository.searchGames(query).map { games ->
            val platforms = games.flatMap { it.platforms }.distinctBy { it.id }
            val platformCounts = platforms
                .groupingBy { it.id }
                .eachCount()

            SearchResult(
                games = games,
                platforms = platformCounts.keys
                    .sortedByDescending { platformCounts[it] }
                    .map { platformId ->
                        platforms.first { it.id == platformId }
                    }
            )
        }
    }
}
