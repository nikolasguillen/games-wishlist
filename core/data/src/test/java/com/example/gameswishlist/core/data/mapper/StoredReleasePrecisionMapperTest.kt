package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.PlatformEntity
import com.example.gameswishlist.core.database.relation.GamePlatformWithDetails
import com.example.gameswishlist.core.database.relation.GameWithAllDetails
import com.example.gameswishlist.core.model.DatePrecision
import com.example.gameswishlist.core.model.GameType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers how [GameWithAllDetails.toGame] reads a stored release-date precision back. Rows written while
 * the app still asked IGDB for the removed `category` field carry no usable scalar — either none at all or
 * the exact-date default the mapper used to fall back to — so a date landing on IGDB's 31 December
 * year-only placeholder has to be read as year-only rather than as a real day. A scalar IGDB actually
 * returned is trusted as-is.
 */
class StoredReleasePrecisionMapperTest {

    /** Midnight UTC on 31 December 2027: IGDB's placeholder for "only the year is known". */
    private val yearOnlyPlaceholder = 1_830_211_200L

    private val pc = PlatformEntity(
        id = 6,
        name = "PC",
        abbreviation = "PC",
        generation = null,
        category = null,
        platformFamily = null
    )

    private fun gameWith(precision: Int?, date: Long?) = GameWithAllDetails(
        game = GameEntity(
            id = 1,
            name = "Cindergate",
            description = "",
            released = null,
            backgroundImage = null,
            rating = 0.0,
            metacritic = null,
            gameTypeId = GameType.MAIN_GAME.id,
            notes = "",
            priority = null,
            status = null,
            url = null,
            detailsFetchedAt = null
        ),
        platformRefs = listOf(
            GamePlatformWithDetails(
                crossRef = GamePlatformCrossRef(
                    gameId = 1,
                    platformId = 6,
                    releaseDate = date,
                    releaseDatePrecision = precision
                ),
                platform = pc
            )
        ),
        genres = emptyList(),
        companyRefs = emptyList(),
        relatedGames = emptyList(),
        engines = emptyList(),
        artworks = emptyList()
    )

    private fun precisionOf(precision: Int?, date: Long?): DatePrecision =
        gameWith(precision, date).toGame().releaseDates.single().precision

    @Test
    fun `no stored scalar on the 31 December placeholder reads as year-only`() {
        assertEquals(DatePrecision.YEAR_ONLY, precisionOf(precision = null, date = yearOnlyPlaceholder))
    }

    @Test
    fun `the old exact-date default on the 31 December placeholder reads as year-only`() {
        assertEquals(DatePrecision.YEAR_ONLY, precisionOf(precision = 0, date = yearOnlyPlaceholder))
    }

    @Test
    fun `a scalar IGDB returned is trusted even on 31 December`() {
        assertEquals(DatePrecision.YEAR_MONTH, precisionOf(precision = 1, date = yearOnlyPlaceholder))
    }

    @Test
    fun `an exact date that is not 31 December stays an exact date`() {
        assertEquals(DatePrecision.EXACT_DATE, precisionOf(precision = 0, date = 1_000L))
    }

    @Test
    fun `no date at all leaves the exact-date default alone`() {
        assertEquals(DatePrecision.EXACT_DATE, precisionOf(precision = null, date = null))
    }
}
