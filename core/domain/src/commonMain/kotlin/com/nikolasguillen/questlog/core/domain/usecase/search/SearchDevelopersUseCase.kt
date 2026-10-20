package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.Developer

/**
 * The most developers one search shows. The catalogue match is "name contains", so a short query can match
 * hundreds of studios; nobody scrolls past the first hundred, and the ranking puts the likely ones first.
 */
internal const val DEVELOPER_RESULTS_LIMIT = 100

/**
 * Use case to search for developers by name.
 *
 * The catalogue returns every company whose name contains the query, in no useful order, so this ranks them:
 * a name equal to the query first, then names that start with it, then the rest; within each group the studio
 * with more games first, then by name. At most [DEVELOPER_RESULTS_LIMIT] are returned.
 */
class SearchDevelopersUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String): AppResult<List<Developer>> {
        return repository.searchDevelopers(query).map { it.rankedFor(query) }
    }
}

/** Orders [this] as [SearchDevelopersUseCase] describes and keeps the first [DEVELOPER_RESULTS_LIMIT]. */
internal fun List<Developer>.rankedFor(query: String): List<Developer> {
    val needle = query.trim()
    return sortedWith(
        compareBy<Developer> { it.matchGroup(needle) }
            .thenByDescending { it.developedGameCount }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    ).take(DEVELOPER_RESULTS_LIMIT)
}

private fun Developer.matchGroup(needle: String): Int = when {
    name.equals(needle, ignoreCase = true) -> 0
    name.startsWith(needle, ignoreCase = true) -> 1
    else -> 2
}
