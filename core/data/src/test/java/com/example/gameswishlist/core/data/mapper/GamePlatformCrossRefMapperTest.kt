package com.example.questlog.core.data.mapper

import com.example.questlog.core.common.DateUtils
import com.example.questlog.core.model.DatePrecision
import com.example.questlog.core.model.Game
import com.example.questlog.core.model.Platform
import com.example.questlog.core.model.ReleaseDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [toGamePlatformCrossRefs] on [Game]: the fallback to the game's own release date when IGDB gave
 * no per-platform dates, and that the per-platform lookup matches on id rather than name.
 */
class GamePlatformCrossRefMapperTest {

    private val pc = Platform(id = 6, name = "PC")
    private val ps5 = Platform(id = 167, name = "PlayStation 5")

    @Test
    fun `no per-platform dates falls back to the game's own release date for every platform`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc, ps5),
            releaseDates = emptyList(),
            releaseDate = "2026-05-20"
        )

        val result = game.toGamePlatformCrossRefs()

        val expectedDate = DateUtils.isoDateToEpochSeconds("2026-05-20")
        assertEquals(setOf(expectedDate), result.map { it.releaseDate }.toSet())
        assertEquals(setOf(0), result.map { it.releaseDatePrecision }.toSet())
    }

    @Test
    fun `a platform with its own date keeps it, a platform without one gets the fallback`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc, ps5),
            releaseDates = listOf(
                ReleaseDate(date = 1_000L, platformId = 167, platformName = "PlayStation 5", precision = DatePrecision.YEAR_MONTH)
            ),
            releaseDate = "2026-05-20"
        )

        val result = game.toGamePlatformCrossRefs()

        val ps5CrossRef = result.single { it.platformId == 167 }
        assertEquals(1_000L, ps5CrossRef.releaseDate)
        assertEquals(1, ps5CrossRef.releaseDatePrecision)

        val pcCrossRef = result.single { it.platformId == 6 }
        assertEquals(DateUtils.isoDateToEpochSeconds("2026-05-20"), pcCrossRef.releaseDate)
        assertEquals(0, pcCrossRef.releaseDatePrecision)
    }

    @Test
    fun `no game release date and no per-platform dates leaves date and precision null`() {
        val game = Game(id = 1, name = "Cindergate", platforms = listOf(pc), releaseDates = emptyList(), releaseDate = null)

        val result = game.toGamePlatformCrossRefs()

        assertNull(result.single().releaseDate)
        assertNull(result.single().releaseDatePrecision)
    }

    @Test
    fun `a fallback date landing on 31 December is treated as year-only, not an exact date`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc),
            releaseDates = emptyList(),
            releaseDate = "2026-12-31"
        )

        val result = game.toGamePlatformCrossRefs()

        assertEquals(2, result.single().releaseDatePrecision)
    }

    @Test
    fun `a fallback date not landing on 31 December stays an exact date`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc),
            releaseDates = emptyList(),
            releaseDate = "2026-12-30"
        )

        val result = game.toGamePlatformCrossRefs()

        assertEquals(0, result.single().releaseDatePrecision)
    }

    @Test
    fun `a platform with its own 31 December date is not affected by the fallback heuristic`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc),
            releaseDates = listOf(
                ReleaseDate(date = 1_000L, platformId = 6, platformName = "PC", precision = DatePrecision.EXACT_DATE)
            ),
            releaseDate = "2026-12-31"
        )

        val result = game.toGamePlatformCrossRefs()

        assertEquals(0, result.single().releaseDatePrecision)
    }

    @Test
    fun `the per-platform lookup matches on id, not on the platform name`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(ps5),
            // Name deliberately mismatched from Platform.name -- IGDB does not guarantee the two strings match.
            releaseDates = listOf(
                ReleaseDate(date = 5_000L, platformId = 167, platformName = "PS5", precision = DatePrecision.EXACT_DATE)
            ),
            releaseDate = null
        )

        val result = game.toGamePlatformCrossRefs()

        assertEquals(5_000L, result.single().releaseDate)
    }
}
