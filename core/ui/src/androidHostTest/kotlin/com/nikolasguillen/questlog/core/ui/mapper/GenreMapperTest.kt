package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.Genre
import com.nikolasguillen.questlog.core.model.KnownGenre
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.genre_role_playing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [toLabelUiText] and the [KnownGenre] table behind it: a known id resolves to its own `genre_*`
 * resource whatever name came with it, any other id keeps IGDB's name, and the fixed defaults of the genre
 * browse row are the five the spec lists.
 */
class GenreMapperTest {

    @Test
    fun `every known genre maps to a string resource and no two share one`() {
        val resources = KnownGenre.entries.map { known ->
            val label = Genre(known.id, known.igdbName).toLabelUiText()
            assertTrue("$known is not a resource", label is UiText.StringResource)
            (label as UiText.StringResource).res
        }

        assertEquals(resources.size, resources.toSet().size)
    }

    @Test
    fun `a known genre resolves to its short label resource`() {
        val label = Genre(12, "Role-playing (RPG)").toLabelUiText()

        assertEquals(UiText.StringResource(Res.string.genre_role_playing), label)
    }

    @Test
    fun `a genre the app has no entry for keeps the name IGDB gave it`() {
        val label = Genre(9999, "Wonder Genre").toLabelUiText()

        assertEquals(UiText.DynamicString("Wonder Genre"), label)
    }

    @Test
    fun `the label depends on the id and not on the name`() {
        val label = Genre(12, "anything").toLabelUiText()

        assertEquals(UiText.StringResource(Res.string.genre_role_playing), label)
    }

    @Test
    fun `known genre ids are unique`() {
        val ids = KnownGenre.entries.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `fromId finds a listed id and returns null for any other`() {
        assertEquals(KnownGenre.ROLE_PLAYING, KnownGenre.fromId(12))
        assertNull(KnownGenre.fromId(9999))
    }

    @Test
    fun `the browse defaults are RPG, Shooter, Adventure, Strategy and Indie in that order`() {
        assertEquals(listOf(12, 5, 31, 15, 32), KnownGenre.BROWSE_DEFAULTS.map { it.id })
    }
}
