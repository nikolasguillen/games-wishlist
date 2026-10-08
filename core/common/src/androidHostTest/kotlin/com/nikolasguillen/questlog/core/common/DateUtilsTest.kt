package com.nikolasguillen.questlog.core.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

/**
 * Pins what [DateUtils] returns today, so the move to `kotlinx-datetime` cannot change a date shown to the
 * user. Results are compared as strings wherever the return type is a date object, because that type is
 * the thing being swapped.
 */
class DateUtilsTest {

    private lateinit var originalLocale: Locale
    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        originalZone = TimeZone.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalZone)
    }

    private fun zone(id: String) = TimeZone.setDefault(TimeZone.getTimeZone(id))

    // 2024-03-15T12:00:00Z, a Friday
    private val noonUtc = 1_710_504_000L

    @Test
    fun `formatUnixTimestamp with a pattern`() {
        assertEquals("2024", DateUtils.formatUnixTimestamp(noonUtc, "yyyy"))
        assertEquals("2024-03-15", DateUtils.formatUnixTimestamp(noonUtc, "yyyy-MM-dd"))
        assertEquals("Mar 2024", DateUtils.formatUnixTimestamp(noonUtc, "MMM yyyy"))
        assertEquals("Fri", DateUtils.formatUnixTimestamp(noonUtc, "EEE"))
        assertEquals("Mar 15", DateUtils.formatUnixTimestamp(noonUtc, "MMM d"))
    }

    @Test
    fun `formatUnixTimestamp defaults to the medium localized style`() {
        assertEquals("Mar 15, 2024", DateUtils.formatUnixTimestamp(noonUtc))
    }

    @Test
    fun `formatUnixTimestamp uses the device time zone for the calendar day`() {
        val lateUtc = 1_710_545_400L // 2024-03-15T23:30:00Z
        val earlyUtc = 1_710_471_600L // 2024-03-15T03:00:00Z

        zone("GMT-08:00")
        assertEquals("2024-03-15", DateUtils.formatUnixTimestamp(lateUtc, "yyyy-MM-dd"))
        assertEquals("2024-03-14", DateUtils.formatUnixTimestamp(earlyUtc, "yyyy-MM-dd"))

        zone("GMT+09:00")
        assertEquals("2024-03-16", DateUtils.formatUnixTimestamp(lateUtc, "yyyy-MM-dd"))
        assertEquals("2024-03-15", DateUtils.formatUnixTimestamp(earlyUtc, "yyyy-MM-dd"))
    }

    @Test
    fun `formatIsoDate formats a valid date and passes bad input through`() {
        assertEquals("Mar 15, 2024", DateUtils.formatIsoDate("2024-03-15"))
        assertNull(DateUtils.formatIsoDate(null))
        assertNull(DateUtils.formatIsoDate(""))
        assertEquals("not a date", DateUtils.formatIsoDate("not a date"))
    }

    @Test
    fun `getYearFromIsoDate`() {
        assertEquals("2024", DateUtils.getYearFromIsoDate("2024-03-15"))
        assertNull(DateUtils.getYearFromIsoDate(null))
        assertNull(DateUtils.getYearFromIsoDate(""))
        assertNull(DateUtils.getYearFromIsoDate("not a date"))
    }

    @Test
    fun `parseIsoDate`() {
        assertEquals("2024-03-15", DateUtils.parseIsoDate("2024-03-15").toString())
        assertNull(DateUtils.parseIsoDate(null))
        assertNull(DateUtils.parseIsoDate(""))
        assertNull(DateUtils.parseIsoDate("2024-13-45"))
        assertNull(DateUtils.parseIsoDate("not a date"))
    }

    @Test
    fun `timestampToLocalDate follows the device time zone`() {
        val lateUtc = 1_710_545_400L // 2024-03-15T23:30:00Z

        zone("GMT-08:00")
        assertEquals("2024-03-15", DateUtils.timestampToLocalDate(lateUtc).toString())

        zone("GMT+09:00")
        assertEquals("2024-03-16", DateUtils.timestampToLocalDate(lateUtc).toString())
    }

    @Test
    fun `isoDateToEpochSeconds is midnight of that day in the device time zone`() {
        assertEquals(1_710_460_800L, DateUtils.isoDateToEpochSeconds("2024-03-15"))

        zone("GMT+09:00")
        assertEquals(1_710_460_800L - 9 * 3_600, DateUtils.isoDateToEpochSeconds("2024-03-15"))

        assertNull(DateUtils.isoDateToEpochSeconds(null))
        assertNull(DateUtils.isoDateToEpochSeconds("not a date"))
    }

    @Test
    fun `isYearOnlyPlaceholder for an ISO string is 31 December`() {
        assertTrue(DateUtils.isYearOnlyPlaceholder("2023-12-31"))
        assertFalse(DateUtils.isYearOnlyPlaceholder("2023-12-30"))
        assertFalse(DateUtils.isYearOnlyPlaceholder("2024-01-01"))
        assertFalse(DateUtils.isYearOnlyPlaceholder(null as String?))
        assertFalse(DateUtils.isYearOnlyPlaceholder("not a date"))
    }

    @Test
    fun `isYearOnlyPlaceholder for a timestamp is checked in UTC, whatever the device zone`() {
        val dec31Utc = 1_703_980_800L // 2023-12-31T00:00:00Z
        val dec30Utc = 1_703_894_400L // 2023-12-30T00:00:00Z

        zone("GMT-08:00") // west of Greenwich the same instant reads as 30 December locally
        assertTrue(DateUtils.isYearOnlyPlaceholder(dec31Utc))
        assertFalse(DateUtils.isYearOnlyPlaceholder(dec30Utc))

        zone("GMT+09:00")
        assertTrue(DateUtils.isYearOnlyPlaceholder(dec31Utc))
        assertFalse(DateUtils.isYearOnlyPlaceholder(null as Long?))
    }
}
