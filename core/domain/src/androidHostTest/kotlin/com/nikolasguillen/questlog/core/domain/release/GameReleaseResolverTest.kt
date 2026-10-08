package com.nikolasguillen.questlog.core.domain.release

import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.ReleaseDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers when a game counts as released ([isReleased], release day included), when its release day has
 * passed ([isPastReleaseDay], release day excluded), and which statuses that unlocks ([canSetStatus]).
 *
 * A coarse date only counts once the whole period it names is over, the earliest platform wins,
 * [Game.releaseDate] is only a fallback for a game with no usable platform date, and a game with no date
 * anywhere is unreleased.
 *
 * [now] is fixed at 2026-09-22T00:00:00Z, read in UTC unless a test says otherwise, so "today" is
 * 2026-09-22 and the current quarter is Q3, ending 2026-09-30.
 */
class GameReleaseResolverTest {

    private val now = LocalDate(2026, 9, 22).atStartOfDayIn(TimeZone.UTC)

    private fun platformDate(
        year: Int,
        month: Int,
        day: Int,
        precision: DatePrecision = DatePrecision.EXACT_DATE,
        platformId: Int = 6
    ) = ReleaseDate(
        date = LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).epochSeconds,
        platformId = platformId,
        platformName = "PC",
        precision = precision
    )

    private fun gameWith(vararg dates: ReleaseDate, releaseDate: String? = null) =
        Game(id = 1, name = "Cindergate", releaseDates = dates.toList(), releaseDate = releaseDate)

    private fun Game.isReleased() = isReleased(now, TimeZone.UTC)

    private fun Game.isPastReleaseDay() = isPastReleaseDay(now, TimeZone.UTC)

    @Test
    fun `an exact date in the past is released`() {
        assertTrue(gameWith(platformDate(2026, 9, 21)).isReleased())
    }

    @Test
    fun `an exact date in the future is not released`() {
        assertFalse(gameWith(platformDate(2026, 9, 23)).isReleased())
    }

    @Test
    fun `a game releasing today is released`() {
        assertTrue(gameWith(platformDate(2026, 9, 22)).isReleased())
    }

    @Test
    fun `a game releasing today is not past its release day`() {
        assertFalse(gameWith(platformDate(2026, 9, 22)).isPastReleaseDay())
    }

    @Test
    fun `a game released yesterday is past its release day`() {
        assertTrue(gameWith(platformDate(2026, 9, 21)).isPastReleaseDay())
    }

    @Test
    fun `a quarter is not released until the quarter ends`() {
        assertFalse(gameWith(platformDate(2026, 7, 1, DatePrecision.QUARTER)).isReleased())
    }

    @Test
    fun `a quarter that has ended is released`() {
        assertTrue(gameWith(platformDate(2026, 4, 1, DatePrecision.QUARTER)).isReleased())
    }

    @Test
    fun `a month is not released until the month ends`() {
        assertFalse(gameWith(platformDate(2026, 9, 1, DatePrecision.YEAR_MONTH)).isReleased())
    }

    @Test
    fun `a month that has ended is released`() {
        assertTrue(gameWith(platformDate(2026, 8, 1, DatePrecision.YEAR_MONTH)).isReleased())
    }

    @Test
    fun `the current year is not released until the year ends`() {
        assertFalse(gameWith(platformDate(2026, 12, 31, DatePrecision.YEAR_ONLY)).isReleased())
    }

    @Test
    fun `a past year is released`() {
        assertTrue(gameWith(platformDate(2025, 12, 31, DatePrecision.YEAR_ONLY)).isReleased())
    }

    @Test
    fun `a TBD platform date is not released`() {
        assertFalse(gameWith(platformDate(2020, 1, 1, DatePrecision.TBD)).isReleased())
    }

    @Test
    fun `the earliest platform release wins`() {
        val game = gameWith(platformDate(2027, 3, 1, platformId = 48), platformDate(2026, 9, 1, platformId = 6))

        assertTrue(game.isReleased())
    }

    @Test
    fun `a coarse platform date is preferred over the main date`() {
        // The main date is IGDB's placeholder for the quarter; taken as an exact day it would read as out.
        val game = gameWith(platformDate(2026, 7, 1, DatePrecision.QUARTER), releaseDate = "2026-07-01")

        assertFalse(game.isReleased())
    }

    @Test
    fun `without platform dates the main date is used`() {
        assertTrue(gameWith(releaseDate = "2026-09-01").isReleased())
    }

    @Test
    fun `a main date landing on 31 December is read as the whole year`() {
        assertFalse(gameWith(releaseDate = "2026-12-31").isReleased())
    }

    @Test
    fun `a game with no date anywhere is not released`() {
        assertFalse(gameWith(ReleaseDate(date = null, platformId = 6, platformName = "PC")).isReleased())
    }

    @Test
    fun `a game with no date anywhere is not past its release day`() {
        assertFalse(gameWith().isPastReleaseDay())
    }

    @Test
    fun `a coarse date is read in UTC whatever the device zone`() {
        // 2026-10-01T00:00Z is Q4's placeholder; in New York that instant is still 30 September, Q3.
        val game = gameWith(platformDate(2026, 10, 1, DatePrecision.QUARTER))
        val midDecember = LocalDate(2026, 12, 15).atStartOfDayIn(TimeZone.UTC)

        assertFalse(game.isReleased(midDecember, TimeZone.of("America/New_York")))
    }

    @Test
    fun `an unreleased game only accepts statuses that do not need a release`() {
        val game = gameWith(platformDate(2026, 10, 1))

        val allowed = GameStatus.entries.filter { game.canSetStatus(it, now, TimeZone.UTC) }

        assertEquals(listOf(GameStatus.WANT_TO_BUY, GameStatus.BOUGHT), allowed)
    }

    @Test
    fun `a released game accepts every status`() {
        val game = gameWith(platformDate(2026, 9, 1))

        assertTrue(GameStatus.entries.all { game.canSetStatus(it, now, TimeZone.UTC) })
    }
}
