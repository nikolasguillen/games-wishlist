package com.nikolasguillen.questlog.core.network

import com.nikolasguillen.questlog.core.network.model.IgdbAuthResponse
import com.nikolasguillen.questlog.core.network.model.IgdbGame
import com.nikolasguillen.questlog.core.network.model.IgdbInvolvedCompany
import com.nikolasguillen.questlog.core.network.model.IgdbPlatform
import com.nikolasguillen.questlog.core.network.model.IgdbPopularityPrimitive
import com.nikolasguillen.questlog.core.network.model.IgdbReleaseDateEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins how IGDB's JSON turns into the network models, so swapping the JSON library cannot change what the
 * app reads: a missing optional field is `null`, an unknown field is ignored, an explicit `null` is `null`,
 * and a missing required field fails. Only [decode] and [decodeList] know which library is in use.
 */
class IgdbDtoDecodingTest {

    private inline fun <reified T : Any> decode(json: String): T = IgdbJson.decodeFromString<T>(json)

    private inline fun <reified T : Any> decodeList(json: String): List<T> = IgdbJson.decodeFromString<List<T>>(json)

    private val fullGame = """
        {
          "id": 1942,
          "name": "The Witcher 3: Wild Hunt",
          "summary": "An open-world RPG.",
          "game_type": 0,
          "first_release_date": 1431993600,
          "cover": { "id": 89386, "url": "//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg" },
          "total_rating": 92.4,
          "total_rating_count": 1500,
          "aggregated_rating": 91,
          "hypes": 12,
          "url": "https://www.igdb.com/games/the-witcher-3-wild-hunt",
          "platforms": [ { "id": 6, "abbreviation": "PC", "name": "PC (Microsoft Windows)", "generation": null, "category": 4, "platform_family": null } ],
          "release_dates": [ { "id": 7, "date": 1431993600, "platform": { "id": 6, "name": "PC (Microsoft Windows)" }, "date_format": 0 } ],
          "genres": [ { "id": 12, "name": "Role-playing (RPG)" } ],
          "involved_companies": [
            { "id": 1, "company": { "id": 908, "name": "CD Projekt RED" }, "developer": true, "publisher": false }
          ],
          "game_engines": [ { "id": 5, "name": "REDengine" } ],
          "parent_game": { "id": 1, "name": "Parent" },
          "dlcs": [ { "id": 2, "name": "Hearts of Stone" } ],
          "expansions": [ { "id": 3, "name": "Blood and Wine" } ],
          "remakes": [ { "id": 4, "name": "Remake" } ],
          "remasters": [ { "id": 5, "name": "Remaster" } ],
          "artworks": [ { "id": 10, "url": "//images.igdb.com/art.jpg" } ],
          "screenshots": [ { "id": 11, "url": "//images.igdb.com/shot.jpg" } ]
        }
    """.trimIndent()

    @Test
    fun `a fully populated game decodes every field`() {
        val game = decode<IgdbGame>(fullGame)

        assertEquals(1942, game.id)
        assertEquals("The Witcher 3: Wild Hunt", game.name)
        assertEquals("An open-world RPG.", game.summary)
        assertEquals(0, game.gameType)
        assertEquals(1_431_993_600L, game.firstReleaseDate)
        assertEquals(89386, game.cover?.id)
        assertEquals("//images.igdb.com/igdb/image/upload/t_thumb/co1wyy.jpg", game.cover?.url)
        assertEquals(92.4, game.totalRating!!, 0.0)
        assertEquals(1500, game.totalRatingCount)
        assertEquals(91.0, game.aggregatedRating!!, 0.0)
        assertEquals(12, game.hypes)
        assertEquals("https://www.igdb.com/games/the-witcher-3-wild-hunt", game.url)
        assertEquals("PC", game.platforms?.single()?.abbreviation)
        assertEquals(4, game.platforms?.single()?.category)
        assertNull(game.platforms?.single()?.generation)
        assertEquals(1_431_993_600L, game.releaseDates?.single()?.date)
        assertEquals(6, game.releaseDates?.single()?.platform?.id)
        assertEquals(0, game.releaseDates?.single()?.dateFormat)
        assertEquals("Role-playing (RPG)", game.genres?.single()?.name)
        assertEquals("CD Projekt RED", game.involvedCompanies?.single()?.company?.name)
        assertEquals(true, game.involvedCompanies?.single()?.developer)
        assertEquals(false, game.involvedCompanies?.single()?.publisher)
        assertEquals("REDengine", game.gameEngines?.single()?.name)
        assertEquals("Parent", game.parentGame?.name)
        assertEquals("Hearts of Stone", game.dlcList?.single()?.name)
        assertEquals("Blood and Wine", game.expansions?.single()?.name)
        assertEquals("Remake", game.remakes?.single()?.name)
        assertEquals("Remaster", game.remasters?.single()?.name)
        assertEquals("//images.igdb.com/art.jpg", game.artworks?.single()?.url)
        assertEquals("//images.igdb.com/shot.jpg", game.screenshots?.single()?.url)
    }

    @Test
    fun `a game with every optional field absent decodes them as null`() {
        val game = decode<IgdbGame>("""{ "id": 1, "name": "Bare" }""")

        assertEquals(1, game.id)
        assertEquals("Bare", game.name)
        assertNull(game.summary)
        assertNull(game.gameType)
        assertNull(game.firstReleaseDate)
        assertNull(game.cover)
        assertNull(game.totalRating)
        assertNull(game.totalRatingCount)
        assertNull(game.aggregatedRating)
        assertNull(game.hypes)
        assertNull(game.url)
        assertNull(game.platforms)
        assertNull(game.releaseDates)
        assertNull(game.genres)
        assertNull(game.involvedCompanies)
        assertNull(game.gameEngines)
        assertNull(game.parentGame)
        assertNull(game.dlcList)
        assertNull(game.expansions)
        assertNull(game.remakes)
        assertNull(game.remasters)
        assertNull(game.artworks)
        assertNull(game.screenshots)
    }

    @Test
    fun `explicit nulls decode as null`() {
        val game = decode<IgdbGame>(
            """{ "id": 1, "name": "Nulls", "summary": null, "cover": null, "platforms": null, "hypes": null, "parent_game": null }"""
        )

        assertNull(game.summary)
        assertNull(game.cover)
        assertNull(game.platforms)
        assertNull(game.hypes)
        assertNull(game.parentGame)
    }

    @Test
    fun `unknown fields are ignored`() {
        val game = decode<IgdbGame>(
            """{ "id": 1, "name": "Extra", "brand_new_field": { "a": 1 }, "another": [1, 2, 3], "flag": true }"""
        )

        assertEquals("Extra", game.name)
    }

    @Test
    fun `a rating written as a whole number reads as a double`() {
        val game = decode<IgdbGame>("""{ "id": 1, "name": "Whole", "total_rating": 90, "aggregated_rating": 80.5 }""")

        assertEquals(90.0, game.totalRating!!, 0.0)
        assertEquals(80.5, game.aggregatedRating!!, 0.0)
    }

    @Test
    fun `a list of games decodes in order`() {
        val games = decodeList<IgdbGame>("""[ { "id": 1, "name": "A" }, { "id": 2, "name": "B" } ]""")

        assertEquals(listOf(1, 2), games.map { it.id })
        assertEquals(listOf("A", "B"), games.map { it.name })
    }

    @Test
    fun `an involved company defaults its roles to false when they are absent and keeps an explicit null`() {
        val absent = decode<IgdbInvolvedCompany>("""{ "id": 1, "company": { "id": 2, "name": "Studio" } }""")
        val explicitNull = decode<IgdbInvolvedCompany>(
            """{ "id": 1, "company": { "id": 2, "name": "Studio" }, "developer": null, "publisher": true }"""
        )

        assertEquals(false, absent.developer)
        assertEquals(false, absent.publisher)
        assertNull(explicitNull.developer)
        assertEquals(true, explicitNull.publisher)
    }

    @Test
    fun `a release date entry with no platform and no date decodes`() {
        val entry = decode<IgdbReleaseDateEntry>("""{ "id": 9, "game": 1942 }""")

        assertEquals(9, entry.id)
        assertEquals(1942, entry.game)
        assertNull(entry.platform)
        assertNull(entry.date)
        assertNull(entry.dateFormat)
    }

    @Test
    fun `a platform needs only an id and a name`() {
        val platform = decode<IgdbPlatform>("""{ "id": 167, "name": "PlayStation 5" }""")

        assertEquals(167, platform.id)
        assertEquals("PlayStation 5", platform.name)
        assertNull(platform.abbreviation)
        assertNull(platform.generation)
        assertNull(platform.category)
        assertNull(platform.platformFamily)
    }

    @Test
    fun `a popularity primitive reads a whole-number value as a double`() {
        val whole = decode<IgdbPopularityPrimitive>("""{ "game_id": 7, "value": 3 }""")
        val fractional = decode<IgdbPopularityPrimitive>("""{ "game_id": 8, "value": 12.5 }""")

        assertEquals(7, whole.gameId)
        assertEquals(3.0, whole.value, 0.0)
        assertEquals(12.5, fractional.value, 0.0)
    }

    @Test
    fun `the auth response reads the token and its lifetime`() {
        val response = decode<IgdbAuthResponse>(
            """{ "access_token": "abc123", "expires_in": 5184000, "token_type": "bearer" }"""
        )

        assertEquals("abc123", response.accessToken)
        assertEquals(5_184_000L, response.expiresIn)
        assertEquals("bearer", response.tokenType)
    }

    @Test
    fun `a missing required field fails`() {
        assertThrows(Exception::class.java) { decode<IgdbGame>("""{ "name": "No id" }""") }
        assertThrows(Exception::class.java) { decode<IgdbGame>("""{ "id": 1 }""") }
        assertTrue(runCatching { decode<IgdbPlatform>("""{ "id": 1 }""") }.isFailure)
        assertFalse(runCatching { decode<IgdbPlatform>("""{ "id": 1, "name": "ok" }""") }.isFailure)
    }
}
