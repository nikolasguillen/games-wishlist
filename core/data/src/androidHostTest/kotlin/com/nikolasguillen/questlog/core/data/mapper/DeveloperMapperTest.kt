package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.network.model.IgdbCompany
import com.nikolasguillen.questlog.core.network.model.IgdbImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pins how an IGDB company becomes a developer search result and a profile. */
class DeveloperMapperTest {

    private val company = IgdbCompany(
        id = 3,
        name = "Naughty Dog",
        description = "A studio.",
        startDate = 473299200,
        logo = IgdbImage(1, "//images.igdb.com/igdb/image/upload/t_thumb/logo.png"),
        developed = listOf(1, 2, 3)
    )

    @Test
    fun `the logo is rewritten to the logo size with the https prefix`() {
        val expected = "https://images.igdb.com/igdb/image/upload/t_logo_med/logo.png"

        assertEquals(expected, company.toDeveloper().logoUrl)
        assertEquals(expected, company.toDeveloperProfile().logoUrl)
        assertNull(company.copy(logo = null).toDeveloper().logoUrl)
    }

    @Test
    fun `the game count is the size of the developed list`() {
        assertEquals(3, company.toDeveloper().developedGameCount)
        assertEquals(0, company.copy(developed = null).toDeveloper().developedGameCount)
    }

    @Test
    fun `a blank description is no description`() {
        assertNull(company.copy(description = "").toDeveloperProfile().description)
        assertEquals("A studio.", company.toDeveloperProfile().description)
    }

    @Test
    fun `the founding year is the UTC year of the start date`() {
        assertEquals(1984, company.toDeveloperProfile().foundedYear)
        assertNull(company.copy(startDate = null).toDeveloperProfile().foundedYear)
    }
}
