package com.nikolasguillen.questlog.core.domain.radar

import com.nikolasguillen.questlog.core.model.DatePrecision
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.days

private val ZONE = TimeZone.of("America/New_York")

/** Covers [resolveNotificationInstant]'s branches. [Game.resolveReleaseDates] is covered where it is used, in [GetRadarTimelineUseCaseTest]. */
class ReleaseDateResolverTest {

    @Test
    fun `exact date today before 9am schedules for 9am local`() {
        val now = LocalDateTime(2026, 3, 15, 7, 0).toInstant(ZONE)

        val result = resolveNotificationInstant(now.epochSeconds, DatePrecision.EXACT_DATE, now, ZONE)

        assertEquals(LocalDateTime(2026, 3, 15, 9, 0).toInstant(ZONE), result)
    }

    @Test
    fun `exact date today after 9am returns now`() {
        val now = LocalDateTime(2026, 3, 15, 14, 30).toInstant(ZONE)

        val result = resolveNotificationInstant(now.epochSeconds, DatePrecision.EXACT_DATE, now, ZONE)

        assertEquals(now, result)
    }

    @Test
    fun `exact date yesterday returns null`() {
        val now = LocalDateTime(2026, 3, 15, 10, 0).toInstant(ZONE)
        val yesterday = (now - 1.days).epochSeconds

        val result = resolveNotificationInstant(yesterday, DatePrecision.EXACT_DATE, now, ZONE)

        assertNull(result)
    }

    @Test
    fun `exact date far future schedules for 9am local on that day`() {
        val now = LocalDateTime(2026, 3, 15, 10, 0).toInstant(ZONE)
        val farFuture = LocalDateTime(2027, 6, 1, 0, 0).toInstant(ZONE).epochSeconds

        val result = resolveNotificationInstant(farFuture, DatePrecision.EXACT_DATE, now, ZONE)

        assertEquals(LocalDateTime(2027, 6, 1, 9, 0).toInstant(ZONE), result)
    }

    @Test
    fun `coarser precisions never schedule regardless of date`() {
        val now = LocalDateTime(2026, 3, 15, 10, 0).toInstant(ZONE)
        val farFuture = LocalDateTime(2027, 6, 1, 0, 0).toInstant(ZONE).epochSeconds

        listOf(DatePrecision.YEAR_MONTH, DatePrecision.QUARTER, DatePrecision.YEAR_ONLY, DatePrecision.TBD)
            .forEach { precision ->
                assertNull(resolveNotificationInstant(farFuture, precision, now, ZONE))
            }
    }

    @Test
    fun `null date never schedules`() {
        val now = LocalDateTime(2026, 3, 15, 10, 0).toInstant(ZONE)

        assertNull(resolveNotificationInstant(null, DatePrecision.EXACT_DATE, now, ZONE))
    }

    @Test
    fun `honors a non-default time zone`() {
        val tokyo = TimeZone.of("Asia/Tokyo")
        val now = LocalDateTime(2026, 3, 15, 7, 0).toInstant(tokyo)

        val result = resolveNotificationInstant(now.epochSeconds, DatePrecision.EXACT_DATE, now, tokyo)

        assertEquals(LocalDateTime(2026, 3, 15, 9, 0).toInstant(tokyo), result)
    }
}
