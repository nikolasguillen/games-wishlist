package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.network.model.IgdbCharacter
import com.nikolasguillen.questlog.core.network.model.IgdbImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pins how an IGDB character becomes a search result and a profile. */
class CharacterMapperTest {

    private val character = IgdbCharacter(
        id = 7,
        name = "Kratos",
        description = "The Ghost of Sparta.",
        mugShot = IgdbImage(1, "//images.igdb.com/igdb/image/upload/t_thumb/abc.jpg"),
        games = listOf(10, 11)
    )

    @Test
    fun `the portrait is rewritten to the cover size with the https prefix`() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/abc.jpg",
            character.toGameCharacter().imageUrl
        )
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/abc.jpg",
            character.toCharacterProfile().imageUrl
        )
    }

    @Test
    fun `a character without a portrait has no image`() {
        assertNull(character.copy(mugShot = null).toGameCharacter().imageUrl)
    }

    @Test
    fun `a blank description is no description`() {
        assertNull(character.copy(description = " \n").toCharacterProfile().description)
        assertNull(character.copy(description = null).toCharacterProfile().description)
        assertEquals("The Ghost of Sparta.", character.toCharacterProfile().description)
    }

    @Test
    fun `absent game ids are an empty list`() {
        assertEquals(emptyList<Int>(), character.copy(games = null).toCharacterProfile().gameIds)
        assertEquals(listOf(10, 11), character.toCharacterProfile().gameIds)
    }
}
