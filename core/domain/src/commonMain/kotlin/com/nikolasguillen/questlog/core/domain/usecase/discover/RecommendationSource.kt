package com.nikolasguillen.questlog.core.domain.usecase.discover

/**
 * One personalised Discover shelf's worth of network fetch, as decided by
 * [GetDiscoverFeedUseCase.recommendationPlan]. Internal to the use case: callers only ever see the
 * resulting [com.nikolasguillen.questlog.core.model.RecommendedShelf], never this plan.
 */
internal sealed interface RecommendationSource {
    /** Fetch more games from this developer — the id backing [com.nikolasguillen.questlog.core.model.ShelfReason.ByDeveloper]. */
    data class Developer(val companyId: Int) : RecommendationSource

    /** Fetch more games in this genre — the id backing [com.nikolasguillen.questlog.core.model.ShelfReason.ByGenre]. */
    data class Genre(val genreId: Int) : RecommendationSource
}
