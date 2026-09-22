package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.network.model.IgdbPlatform
import com.example.gameswishlist.core.network.model.IgdbReleaseDateEntry
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [toGamePlatformCrossRefs] on `List<IgdbReleaseDateEntry>`: raw `category` passthrough (no
 * [com.example.gameswishlist.core.model.DatePrecision] conversion at this boundary), duplicate
 * (game, platform) rows collapsing to the earliest date, and dropping a row with no platform object since
 * a cross-ref needs a platform id.
 */
class RadarReleaseDateMapperTest {

    private fun entry(
        id: Int,
        game: Int,
        platform: IgdbPlatform?,
        date: Long?,
        category: Int?
    ) = IgdbReleaseDateEntry(id = id, game = game, platform = platform, date = date, category = category)

    private fun platform(id: Int, name: String = "Platform $id") =
        IgdbPlatform(id = id, abbreviation = null, name = name, generation = null, category = null, platformFamily = null)

    @Test
    fun `category is carried through unchanged as the raw IGDB value`() {
        val result = listOf(entry(id = 1, game = 100, platform = platform(6), date = 1_000L, category = 3))
            .toGamePlatformCrossRefs()

        assertEquals(3, result.single().first.releaseDatePrecision)
    }

    @Test
    fun `duplicate regional rows for the same game and platform collapse to the earliest date`() {
        val result = listOf(
            entry(id = 1, game = 100, platform = platform(6), date = 2_000L, category = 0),
            entry(id = 2, game = 100, platform = platform(6), date = 1_000L, category = 0),
            entry(id = 3, game = 100, platform = platform(6), date = 3_000L, category = 0)
        ).toGamePlatformCrossRefs()

        assertEquals(1, result.size)
        assertEquals(1_000L, result.single().first.releaseDate)
    }

    @Test
    fun `rows for different platforms of the same game stay separate`() {
        val result = listOf(
            entry(id = 1, game = 100, platform = platform(6), date = 1_000L, category = 0),
            entry(id = 2, game = 100, platform = platform(167), date = 2_000L, category = 0)
        ).toGamePlatformCrossRefs()

        assertEquals(setOf(6, 167), result.map { it.first.platformId }.toSet())
    }

    @Test
    fun `a row with no platform object is dropped since a cross-ref needs a platform id`() {
        val result = listOf(
            entry(id = 1, game = 100, platform = null, date = 1_000L, category = 0),
            entry(id = 2, game = 100, platform = platform(6), date = 1_000L, category = 0)
        ).toGamePlatformCrossRefs()

        assertEquals(1, result.size)
        assertEquals(6, result.single().first.platformId)
    }

    @Test
    fun `the paired PlatformEntity backfills an uncached platform`() {
        val result = listOf(entry(id = 1, game = 100, platform = platform(6, "PC"), date = 1_000L, category = 0))
            .toGamePlatformCrossRefs()

        val platformEntity = result.single().second
        assertEquals("PC", platformEntity?.name)
    }
}
