package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.domain.usecase.discover.toTasteProfile
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.KnownGenre
import com.nikolasguillen.questlog.core.model.TasteProfile
import com.nikolasguillen.questlog.core.model.TasteSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** How many genre chips the "Browse by genre" row shows. The row is always exactly this long. */
private const val BROWSE_GENRE_COUNT = 5

/**
 * Use case that picks the genres offered in Search's "Browse by genre" row.
 *
 * The row leads with the genres the user's saved games point to, strongest first, and is padded with
 * [KnownGenre.BROWSE_DEFAULTS] until it has [BROWSE_GENRE_COUNT] distinct genres. A user with no saved games
 * therefore sees the defaults, and nobody ever sees a short row.
 *
 * Re-emits when the library changes, but only when that changes the row.
 */
class GetBrowseGenresUseCase(
    private val repository: GameRepository
) {
    /**
     * @return A flow of exactly five distinct genres, in the order the chips appear.
     */
    operator fun invoke(): Flow<List<Genre>> {
        return repository.getSavedGames()
            .map { games -> games.toBrowseGenres() }
            .distinctUntilChanged()
            // The row is a shortcut, not something worth an error state: if the library cannot be read, the
            // defaults are a perfectly good row. The catch ends the flow, which is acceptable for a failure
            // that should not happen -- the row then stays on the defaults until the screen is recreated.
            .catch { emit(defaultGenres()) }
    }

    private fun List<Game>.toBrowseGenres(): List<Genre> {
        val profile = if (isEmpty()) TasteProfile.EMPTY else toTasteProfile()
        val names = flatMap { it.genres }.associateBy { it.id }

        // Positive weights only: a genre whose only saved games were dropped has a negative weight, and
        // offering it as a shortcut would suggest what the user has just turned down. The id is the last
        // tie-break so two genres with equal weight and count always come out in the same order, instead of
        // the row reshuffling on its own between emissions.
        val taste = profile.genres.entries
            .filter { it.value.weight > 0.0 }
            .sortedWith(
                compareByDescending<Map.Entry<Int, TasteSignal>> { it.value.weight }
                    .thenByDescending { it.value.count }
                    .thenBy { it.key }
            )
            .mapNotNull { names[it.key] }
            .take(BROWSE_GENRE_COUNT)

        val present = taste.mapTo(mutableSetOf()) { it.id }
        val padding = defaultGenres().filter { it.id !in present }
        return (taste + padding).take(BROWSE_GENRE_COUNT)
    }

    private fun defaultGenres(): List<Genre> =
        KnownGenre.BROWSE_DEFAULTS.map { Genre(it.id, it.igdbName) }
}
