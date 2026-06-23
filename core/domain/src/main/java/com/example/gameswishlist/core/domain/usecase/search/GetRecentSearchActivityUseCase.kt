package com.example.gameswishlist.core.domain.usecase.search

import com.example.gameswishlist.core.domain.repository.GameRepository
import com.example.gameswishlist.core.model.RecentSearchActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Composite Use Case that combines search history queries and recently viewed games
 * into a single stream of activity.
 */
class GetRecentSearchActivityUseCase @Inject constructor(
    private val repository: GameRepository
) {
    /**
     * Returns a flow that emits a new [RecentSearchActivity] whenever either
     * the search history or the recently viewed games are updated.
     */
    operator fun invoke(): Flow<RecentSearchActivity> {
        return combine(
            repository.getSearchHistory(),
            repository.getRecentlyViewedGames()
        ) { queries, games ->
            RecentSearchActivity(
                queries = queries,
                games = games
            )
        }
    }
}
