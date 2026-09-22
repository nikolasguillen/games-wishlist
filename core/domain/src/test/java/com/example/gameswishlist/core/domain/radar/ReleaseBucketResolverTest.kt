package com.example.gameswishlist.core.domain.radar

import com.example.gameswishlist.core.model.DatePrecision
import com.example.gameswishlist.core.model.ReleaseBucket
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [resolveBucket]'s calendar-relative boundaries (this ISO week, this calendar month, the 3
 * calendar months after it), the already-released cutoff, TBD/null-date handling, and that the same
 * instant can resolve to a different bucket depending on the time zone passed in.
 *
 * [now] is fixed at 2026-09-22T00:00:00Z, a Tuesday, so the boundaries below are hand-computed rather than
 * derived from wall-clock time: this ISO week ends Sunday 2026-09-27, this month ends 2026-09-30, and the
 * 3 months after it end 2026-12-31.
 */
class ReleaseBucketResolverTest {

    private val now = LocalDate(2026, 9, 22).atStartOfDayIn(TimeZone.UTC)

    private fun epochSecondsOf(year: Int, month: Int, day: Int): Long =
        LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).epochSeconds

    private fun resolve(year: Int, month: Int, day: Int, precision: DatePrecision = DatePrecision.EXACT_DATE) =
        resolveBucket(epochSecondsOf(year, month, day), precision, now, TimeZone.UTC)

    @Test
    fun `a date before today is already released and dropped`() {
        assertNull(resolve(2026, 9, 21))
    }

    @Test
    fun `today falls in this week`() {
        assertEquals(ReleaseBucket.THIS_WEEK, resolve(2026, 9, 22))
    }

    @Test
    fun `the end of the ISO week is still this week`() {
        assertEquals(ReleaseBucket.THIS_WEEK, resolve(2026, 9, 27))
    }

    @Test
    fun `the day after the ISO week rolls into this month`() {
        assertEquals(ReleaseBucket.THIS_MONTH, resolve(2026, 9, 28))
    }

    @Test
    fun `the last day of the month is still this month`() {
        assertEquals(ReleaseBucket.THIS_MONTH, resolve(2026, 9, 30))
    }

    @Test
    fun `the first day of the next month rolls into next 3 months`() {
        assertEquals(ReleaseBucket.NEXT_3_MONTHS, resolve(2026, 10, 1))
    }

    @Test
    fun `the last day of the third following month is still next 3 months`() {
        assertEquals(ReleaseBucket.NEXT_3_MONTHS, resolve(2026, 12, 31))
    }

    @Test
    fun `the day after the third following month rolls into later`() {
        assertEquals(ReleaseBucket.LATER, resolve(2027, 1, 1))
    }

    @Test
    fun `TBD precision is always TBA regardless of the date`() {
        assertEquals(ReleaseBucket.TBA, resolve(2026, 9, 25, DatePrecision.TBD))
    }

    @Test
    fun `a null date is TBA regardless of precision`() {
        assertEquals(
            ReleaseBucket.TBA,
            resolveBucket(null, DatePrecision.EXACT_DATE, now, TimeZone.UTC)
        )
    }

    @Test
    fun `the same instant can resolve to a different bucket depending on the time zone`() {
        // 2026-09-28T00:30:00Z: already Monday in UTC (past this week's Sunday close), but still Sunday
        // night the evening before in America/New_York (UTC-4 in September).
        val instant = epochSecondsOf(2026, 9, 28) + 1_800

        assertEquals(
            ReleaseBucket.THIS_MONTH,
            resolveBucket(instant, DatePrecision.EXACT_DATE, now, TimeZone.UTC)
        )
        assertEquals(
            ReleaseBucket.THIS_WEEK,
            resolveBucket(instant, DatePrecision.EXACT_DATE, now, TimeZone.of("America/New_York"))
        )
    }
}
