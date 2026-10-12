package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.SearchResult

/**
 * Use case behind Search's genre quick-browse: the popular, recent, released games of one genre, in the same
 * [SearchResult] shape a text search returns, so the screen renders both the same way.
 */
class BrowseGenreUseCase(
    private val repository: GameRepository
) {
    /**
     * Browses [genreId].
     *
     * The games keep the order the server returned them in, which is the popularity ranking. The browsed genre
     * is left out of the genre facet because every result carries it: offering it as a filter would narrow
     * nothing.
     *
     * @param genreId The IGDB id of the genre to browse.
     */
    suspend operator fun invoke(genreId: Int): AppResult<SearchResult> {
        return repository.getPopularGamesByGenre(genreId).map { it.toSearchResult(excludedGenreId = genreId) }
    }
}
