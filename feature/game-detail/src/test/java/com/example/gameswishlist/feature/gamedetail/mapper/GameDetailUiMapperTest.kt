package com.example.gameswishlist.feature.gamedetail.mapper

import com.example.gameswishlist.core.common.DateUtils
import com.example.gameswishlist.core.model.DatePrecision
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.ReleaseDate
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.R
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the release-date formatting in [Game.toUiModel]: both the per-platform dates
 * ([com.example.gameswishlist.feature.gamedetail.model.AvailabilityUiModel.detailedDates]) and the
 * game-level main date must respect precision instead of always showing a full day+month+year - a date
 * IGDB only ever knew to the year or the quarter would otherwise show a made-up day.
 */
class GameDetailUiMapperTest {

    private val pc = Platform(id = 6, name = "PC")

    @Test
    fun `a year-only platform date shows only the year`() {
        val date = DateUtils.isoDateToEpochSeconds("2027-06-15")!!
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc),
            releaseDates = listOf(
                ReleaseDate(date = date, platformId = 6, platformName = "PC", precision = DatePrecision.YEAR_ONLY)
            )
        )

        val label = game.toUiModel().availability.detailedDates.single().date

        assertEquals(UiText.DynamicString("2027"), label)
    }

    @Test
    fun `a quarter platform date shows the quarter and the year`() {
        val date = DateUtils.isoDateToEpochSeconds("2027-08-01")!! // August -> Q3
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc),
            releaseDates = listOf(
                ReleaseDate(date = date, platformId = 6, platformName = "PC", precision = DatePrecision.QUARTER)
            )
        )

        val label = game.toUiModel().availability.detailedDates.single().date

        assertEquals(UiText.StringResource(R.string.quarter_format, 3, 2027), label)
    }

    @Test
    fun `an exact platform date shows the full day, month and year`() {
        val date = DateUtils.isoDateToEpochSeconds("2027-06-15")!!
        val game = Game(
            id = 1,
            name = "Cindergate",
            platforms = listOf(pc),
            releaseDates = listOf(
                ReleaseDate(date = date, platformId = 6, platformName = "PC", precision = DatePrecision.EXACT_DATE)
            )
        )

        val label = game.toUiModel().availability.detailedDates.single().date

        assertEquals(UiText.DynamicString(DateUtils.formatUnixTimestamp(date)), label)
    }

    @Test
    fun `a main release date landing on 31 December shows only the year`() {
        val game = Game(id = 1, name = "Cindergate", platforms = listOf(pc), releaseDate = "2027-12-31")

        val mainDate = game.toUiModel().availability.mainDate

        assertEquals(UiText.DynamicString("2027"), mainDate)
    }

    @Test
    fun `a main release date not landing on 31 December shows the full date`() {
        val game = Game(id = 1, name = "Cindergate", platforms = listOf(pc), releaseDate = "2027-06-15")

        val mainDate = game.toUiModel().availability.mainDate

        assertEquals(UiText.DynamicString(DateUtils.formatIsoDate("2027-06-15")!!), mainDate)
    }
}
