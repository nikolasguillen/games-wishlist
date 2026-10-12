package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [toSearchResult], the facet derivation a text search and a genre browse share: distinct platforms and
 * genres ordered by how many games carry them, the optional exclusion of one genre, and the games themselves
 * left exactly as given.
 */
class SearchResultFacetsTest {

    private val rpg = Genre(12, "Role-playing (RPG)")
    private val indie = Genre(32, "Indie")
    private val shooter = Genre(5, "Shooter")
    private val pc = Platform(6, "PC")
    private val switch = Platform(130, "Switch")

    private fun game(id: Int, genres: List<Genre> = emptyList(), platforms: List<Platform> = emptyList()) =
        Game(id = id, name = "Game $id", genres = genres, platforms = platforms)

    @Test
    fun `platforms and genres are distinct`() {
        val games = listOf(
            game(1, genres = listOf(rpg), platforms = listOf(pc)),
            game(2, genres = listOf(rpg), platforms = listOf(pc))
        )

        val result = games.toSearchResult()

        assertEquals(listOf(pc), result.platforms)
        assertEquals(listOf(rpg), result.genres)
    }

    @Test
    fun `facets are ordered by how many games carry them, most first`() {
        val games = listOf(
            game(1, genres = listOf(indie), platforms = listOf(switch)),
            game(2, genres = listOf(rpg, indie), platforms = listOf(pc, switch)),
            game(3, genres = listOf(rpg, indie), platforms = listOf(switch))
        )

        val result = games.toSearchResult()

        assertEquals(listOf(indie, rpg), result.genres)
        assertEquals(listOf(switch, pc), result.platforms)
    }

    @Test
    fun `an excluded genre leaves the genre facet but not the games`() {
        val games = listOf(
            game(1, genres = listOf(rpg, indie)),
            game(2, genres = listOf(rpg, shooter))
        )

        val result = games.toSearchResult(excludedGenreId = 12)

        assertEquals(setOf(indie, shooter), result.genres.toSet())
        assertEquals(games, result.games)
        assertTrue(result.games.all { game -> game.genres.any { it.id == 12 } })
    }

    @Test
    fun `without an exclusion every genre is offered`() {
        val games = listOf(game(1, genres = listOf(rpg, indie)))

        assertEquals(setOf(rpg, indie), games.toSearchResult().genres.toSet())
    }

    @Test
    fun `the games keep the order they came in`() {
        val games = listOf(game(9), game(2), game(5))

        assertEquals(listOf(9, 2, 5), games.toSearchResult().games.map { it.id })
    }

    @Test
    fun `no games gives empty facets`() {
        val result = emptyList<Game>().toSearchResult(excludedGenreId = 12)

        assertEquals(emptyList<Game>(), result.games)
        assertEquals(emptyList<Platform>(), result.platforms)
        assertEquals(emptyList<Genre>(), result.genres)
    }
}
