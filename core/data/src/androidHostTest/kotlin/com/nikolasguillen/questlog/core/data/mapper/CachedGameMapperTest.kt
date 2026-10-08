package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.database.entity.CompanyEntity
import com.nikolasguillen.questlog.core.database.entity.GenreEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.relation.CachedGameCompanyWithDetails
import com.nikolasguillen.questlog.core.database.relation.CachedGameWithDetails
import com.nikolasguillen.questlog.core.model.Company
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameType
import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the cache entity <-> domain round trip for a game shown in a generic Discover lane: platforms,
 * genres, developers and publishers must all survive the round trip, since a game saved straight off a
 * cache-served card persists whatever [Game] the cache produced -- a thin round trip would silently
 * starve the taste profile the recommended shelves are built from (research.md D2).
 */
class CachedGameMapperTest {

    private val pc = Platform(id = 6, name = "PC")
    private val ps5 = Platform(id = 167, name = "PlayStation 5")
    private val rpg = Genre(id = 12, name = "RPG")
    private val cdProjektRed = Company(id = 100, name = "CD Projekt Red")
    private val bandaiNamco = Company(id = 200, name = "Bandai Namco")

    private val sourceGame = Game(
        id = 42,
        name = "Cindergate",
        description = "A grim RPG.",
        releaseDate = "2026-05-20",
        backgroundImage = "https://example.com/cover.jpg",
        rating = 87.5,
        ratingCount = 340,
        hypes = 512,
        metaCritic = 91,
        platforms = listOf(pc, ps5),
        genres = listOf(rpg),
        developers = listOf(cdProjektRed),
        publishers = listOf(bandaiNamco),
        gameType = GameType.MAIN_GAME,
        url = "https://igdb.com/games/cindergate"
    )

    @Test
    fun `a Game round-trips through the cache entities with every field intact`() {
        val entity = sourceGame.toCachedGameEntity()
        val platformRefs = sourceGame.toCachedGamePlatformCrossRefs()
        val genreRefs = sourceGame.toCachedGameGenreCrossRefs()
        val companyRefs = sourceGame.toCachedGameCompanyCrossRefs()

        val withDetails = CachedGameWithDetails(
            game = entity,
            platforms = listOf(pc, ps5).map { PlatformEntity(it.id, it.name, null, null, null, null) },
            genres = listOf(rpg).map { GenreEntity(it.id, it.name) },
            companyRefs = companyRefs.map { crossRef ->
                val company = if (crossRef.companyId == cdProjektRed.id) cdProjektRed else bandaiNamco
                CachedGameCompanyWithDetails(
                    crossRef = crossRef,
                    company = CompanyEntity(company.id, company.name)
                )
            }
        )

        val result = withDetails.toGame()

        assertEquals(sourceGame.id, result.id)
        assertEquals(sourceGame.name, result.name)
        assertEquals(sourceGame.description, result.description)
        assertEquals(sourceGame.releaseDate, result.releaseDate)
        assertEquals(sourceGame.backgroundImage, result.backgroundImage)
        assertEquals(sourceGame.rating, result.rating, 0.0)
        assertEquals(sourceGame.ratingCount, result.ratingCount)
        assertEquals(sourceGame.hypes, result.hypes)
        assertEquals(sourceGame.metaCritic, result.metaCritic)
        assertEquals(sourceGame.gameType, result.gameType)
        assertEquals(sourceGame.url, result.url)
        assertEquals(sourceGame.platforms.toSet(), result.platforms.toSet())
        assertEquals(sourceGame.genres.toSet(), result.genres.toSet())
        assertEquals(sourceGame.developers.toSet(), result.developers.toSet())
        assertEquals(sourceGame.publishers.toSet(), result.publishers.toSet())
        assertEquals(2, platformRefs.size)
        assertEquals(1, genreRefs.size)
        assertEquals(2, companyRefs.size)
    }

    @Test
    fun `a company that is both developer and publisher gets one cross-ref with both flags set`() {
        val game = sourceGame.copy(developers = listOf(cdProjektRed), publishers = listOf(cdProjektRed))

        val companyRefs = game.toCachedGameCompanyCrossRefs()

        val crossRef = companyRefs.single()
        assertEquals(cdProjektRed.id, crossRef.companyId)
        assertEquals(true, crossRef.isDeveloper)
        assertEquals(true, crossRef.isPublisher)
    }

    @Test
    fun `every user-owned field is left at its default, since CachedGameEntity carries none of them`() {
        val withDetails = CachedGameWithDetails(
            game = sourceGame.toCachedGameEntity(),
            platforms = emptyList(),
            genres = emptyList(),
            companyRefs = emptyList()
        )

        val result = withDetails.toGame()

        assertEquals("", result.notes)
        assertEquals(null, result.priority)
        assertEquals(null, result.status)
        assertEquals(null, result.lastViewedAt)
        assertEquals(null, result.detailsFetchedAt)
        assertEquals(false, result.isWishlisted)
    }
}
