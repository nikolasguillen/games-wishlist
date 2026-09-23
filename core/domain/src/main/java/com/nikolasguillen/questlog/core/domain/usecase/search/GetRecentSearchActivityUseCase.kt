package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.RecentSearchActivity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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
    @OptIn(FlowPreview::class)
    operator fun invoke(): Flow<RecentSearchActivity> {
        return combine(
            repository.getRecentSearchHistory(),
            repository.getRecentlyViewedGames().debounce(300.milliseconds)
        ) { queries, games ->
            RecentSearchActivity(
                queries = queries,
                games = games
            )
        }
    }
}
