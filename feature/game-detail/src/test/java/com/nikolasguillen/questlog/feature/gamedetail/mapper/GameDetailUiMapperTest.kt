package com.nikolasguillen.questlog.feature.gamedetail.mapper

import com.nikolasguillen.questlog.core.common.DateUtils
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.model.ReleaseDate
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.gamedetail.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

/**
 * Covers the release-date formatting in [Game.toUiModel]: both the per-platform dates
 * ([com.nikolasguillen.questlog.feature.gamedetail.model.AvailabilityUiModel.detailedDates]) and the
 * game-level main date must respect precision instead of always showing a full day+month+year - a date
 * IGDB only ever knew to the year or the quarter would otherwise show a made-up day.
 *
 * Also covers how the release rule reaches the screen: which status chips are disabled, the hint that
 * explains them, and whether the release reminder is still offered. The rule itself is tested in
 * `core:domain`'s `GameReleaseResolverTest`.
 */
class GameDetailUiMapperTest {

    private val pc = Platform(id = 6, name = "PC")

    /** Midnight of 2026-09-22 in the device zone, so "today" is that calendar day wherever the test runs. */
    private val now = Instant.fromEpochSeconds(DateUtils.isoDateToEpochSeconds("2026-09-22")!!)

    private val releaseGatedStatuses = GameStatus.entries.filter { it.requiresRelease }.map { it.id }.toSet()

    private fun exactPcDate(isoDate: String) = ReleaseDate(
        date = DateUtils.isoDateToEpochSeconds(isoDate),
        platformId = 6,
        platformName = "PC",
        precision = DatePrecision.EXACT_DATE
    )

    private fun Game.personalDetails() = toUiModel(isNotificationEnabled = false, now = now).personalDetails

    private fun Game.disabledStatusIds() =
        personalDetails().availableStatuses.filterNot { it.enabled }.map { it.id }.toSet()

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

        val label = game.toUiModel(isNotificationEnabled = false, now = now).availability.detailedDates.single().date

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

        val label = game.toUiModel(isNotificationEnabled = false, now = now).availability.detailedDates.single().date

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

        val label = game.toUiModel(isNotificationEnabled = false, now = now).availability.detailedDates.single().date

        assertEquals(UiText.DynamicString(DateUtils.formatUnixTimestamp(date)), label)
    }

    @Test
    fun `a main release date landing on 31 December shows only the year`() {
        val game = Game(id = 1, name = "Cindergate", platforms = listOf(pc), releaseDate = "2027-12-31")

        val mainDate = game.toUiModel(isNotificationEnabled = false, now = now).availability.mainDate

        assertEquals(UiText.DynamicString("2027"), mainDate)
    }

    @Test
    fun `a main release date not landing on 31 December shows the full date`() {
        val game = Game(id = 1, name = "Cindergate", platforms = listOf(pc), releaseDate = "2027-06-15")

        val mainDate = game.toUiModel(isNotificationEnabled = false, now = now).availability.mainDate

        assertEquals(UiText.DynamicString(DateUtils.formatIsoDate("2027-06-15")!!), mainDate)
    }

    @Test
    fun `an unreleased game disables only the statuses that need a release`() {
        val game = Game(id = 1, name = "Cindergate", releaseDate = "2026-10-15")

        assertEquals(releaseGatedStatuses, game.disabledStatusIds())
    }

    @Test
    fun `an unreleased game explains why statuses are disabled`() {
        val game = Game(id = 1, name = "Cindergate", releaseDate = "2026-10-15")

        assertEquals(
            UiText.StringResource(R.string.status_locked_until_release),
            game.personalDetails().lockedStatusesHint
        )
    }

    @Test
    fun `a released game enables every status and shows no hint`() {
        val game = Game(id = 1, name = "Cindergate", releaseDate = "2026-09-01")

        assertTrue(game.disabledStatusIds().isEmpty())
        assertNull(game.personalDetails().lockedStatusesHint)
    }

    @Test
    fun `a game without a main date falls back to its platform dates`() {
        val game = Game(id = 1, name = "Cindergate", releaseDates = listOf(exactPcDate("2026-09-01")))

        assertTrue(game.disabledStatusIds().isEmpty())
    }

    @Test
    fun `a game with no date anywhere disables the statuses that need a release`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            releaseDates = listOf(
                ReleaseDate(date = null, platformId = 6, platformName = "PC", precision = DatePrecision.TBD)
            )
        )

        assertEquals(releaseGatedStatuses, game.disabledStatusIds())
        assertNotNull(game.personalDetails().lockedStatusesHint)
    }

    @Test
    fun `the current status stays enabled after its release slips, so it can be cleared`() {
        val game = Game(id = 1, name = "Cindergate", releaseDate = "2026-10-15", status = GameStatus.PLAYING)

        val playing = game.personalDetails().availableStatuses.single { it.id == GameStatus.PLAYING.id }

        assertTrue(playing.enabled)
    }

    @Test
    fun `a saved game can still get a reminder on its release day`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            isWishlisted = true,
            releaseDates = listOf(exactPcDate("2026-09-22"))
        )

        assertTrue(game.toUiModel(isNotificationEnabled = false, now = now).isNotificationAvailable)
    }

    @Test
    fun `a saved game cannot get a reminder once its release day has passed`() {
        val game = Game(
            id = 1,
            name = "Cindergate",
            isWishlisted = true,
            releaseDates = listOf(exactPcDate("2026-09-21"))
        )

        assertFalse(game.toUiModel(isNotificationEnabled = false, now = now).isNotificationAvailable)
    }

    private fun hypesLabelFor(count: Int): UiText? =
        Game(id = 1, name = "Cindergate", hypes = count)
            .toUiModel(isNotificationEnabled = false, now = now)
            .rating?.hypes

    @Test
    fun `large counts are abbreviated to one decimal, rounding half up, with the same thresholds as ever`() {
        val expected = mapOf(
            1 to "1",
            999 to "999",
            1_000 to "1.0K",
            1_049 to "1.0K",
            1_050 to "1.1K",
            1_150 to "1.2K",
            1_250 to "1.3K",
            1_350 to "1.4K",
            999_949 to "999.9K",
            // Still on the K branch, so it prints four digits instead of rolling over to "1.0M".
            999_950 to "1000.0K",
            999_999 to "1000.0K",
            1_000_000 to "1.0M",
            1_049_999 to "1.0M",
            1_050_000 to "1.1M",
            2_340_000 to "2.3M",
            Int.MAX_VALUE to "2147.5M"
        )

        expected.forEach { (count, text) ->
            assertEquals("hypes = $count", UiText.DynamicString(text), hypesLabelFor(count))
        }
    }

    @Test
    fun `the rating count is abbreviated the same way as the hypes`() {
        val rating = Game(id = 1, name = "Cindergate", ratingCount = 12_500)
            .toUiModel(isNotificationEnabled = false, now = now)
            .rating

        assertEquals(UiText.DynamicString("12.5K"), rating?.ratingCount)
    }
}
