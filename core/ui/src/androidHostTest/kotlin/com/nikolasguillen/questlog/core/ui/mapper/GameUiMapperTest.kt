package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.date_ordinal_format
import com.nikolasguillen.questlog.core.ui.resources.release_date_format
import com.nikolasguillen.questlog.core.ui.resources.release_date_tba
import com.nikolasguillen.questlog.core.ui.resources.suffix_nd
import com.nikolasguillen.questlog.core.ui.resources.suffix_rd
import com.nikolasguillen.questlog.core.ui.resources.suffix_st
import com.nikolasguillen.questlog.core.ui.resources.suffix_th
import org.jetbrains.compose.resources.StringResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pins how [toGameItem] spells a release date, so swapping the date library underneath it cannot change
 * what a card says: an English month abbreviation, the day with its ordinal suffix, and the year.
 */
class GameUiMapperTest {

    private fun gameReleasedOn(releaseDate: String?) = Game(id = 1, name = "Cindergate", releaseDate = releaseDate)

    private fun ordinalDate(month: String, day: Int, suffix: StringResource, year: Int) = UiText.StringResource(
        Res.string.release_date_format,
        UiText.StringResource(Res.string.date_ordinal_format, month, day, UiText.StringResource(suffix), year)
    )

    @Test
    fun `a release date shows the month, the day with its ordinal suffix, and the year`() {
        assertEquals(ordinalDate("Mar", 1, Res.string.suffix_st, 2024), gameReleasedOn("2024-03-01").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 2, Res.string.suffix_nd, 2024), gameReleasedOn("2024-03-02").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 3, Res.string.suffix_rd, 2024), gameReleasedOn("2024-03-03").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 4, Res.string.suffix_th, 2024), gameReleasedOn("2024-03-04").toGameItem().releaseDateText)
    }

    @Test
    fun `the teens and the days after them get the right suffix`() {
        assertEquals(ordinalDate("Mar", 11, Res.string.suffix_th, 2024), gameReleasedOn("2024-03-11").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 12, Res.string.suffix_th, 2024), gameReleasedOn("2024-03-12").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 13, Res.string.suffix_th, 2024), gameReleasedOn("2024-03-13").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 21, Res.string.suffix_st, 2024), gameReleasedOn("2024-03-21").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 22, Res.string.suffix_nd, 2024), gameReleasedOn("2024-03-22").toGameItem().releaseDateText)
        assertEquals(ordinalDate("Mar", 23, Res.string.suffix_rd, 2024), gameReleasedOn("2024-03-23").toGameItem().releaseDateText)
    }

    @Test
    fun `every month is abbreviated in English, whatever the device language`() {
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        months.forEachIndexed { index, abbreviation ->
            val iso = "2024-%02d-15".format(index + 1)
            assertEquals(
                iso,
                ordinalDate(abbreviation, 15, Res.string.suffix_th, 2024),
                gameReleasedOn(iso).toGameItem().releaseDateText
            )
        }
    }

    @Test
    fun `the release year comes from the same date`() {
        assertEquals("2024", gameReleasedOn("2024-03-01").toGameItem().releaseYear)
    }

    @Test
    fun `a missing, partial or malformed date falls back to TBA and no year`() {
        val tba = UiText.StringResource(Res.string.release_date_tba)

        listOf(null, "", "2024", "2024-13-45", "not a date").forEach { releaseDate ->
            val item = gameReleasedOn(releaseDate).toGameItem()
            assertEquals("releaseDate = $releaseDate", tba, item.releaseDateText)
            assertNull("releaseDate = $releaseDate", item.releaseYear)
        }
    }
}
