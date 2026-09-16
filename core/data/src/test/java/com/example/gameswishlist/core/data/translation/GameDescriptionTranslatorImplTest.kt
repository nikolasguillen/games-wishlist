package com.example.gameswishlist.core.data.translation

import com.example.gameswishlist.core.ai.GeminiNanoClient
import com.example.gameswishlist.core.database.dao.TranslationDao
import com.example.gameswishlist.core.database.entity.TranslatedDescriptionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Covers [GameDescriptionTranslatorImpl]: the cache short-circuit in [GameDescriptionTranslatorImpl.translate],
 * the length/blank guard before Gemini Nano is ever called, and the two-part check in
 * [GameDescriptionTranslatorImpl.isSupported].
 */
class GameDescriptionTranslatorImplTest {

    private val geminiNanoClient = mockk<GeminiNanoClient>()
    private val translationDao = mockk<TranslationDao>(relaxed = true)

    private val translator = GameDescriptionTranslatorImpl(geminiNanoClient, translationDao)

    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ITALIAN)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `cache hit with matching hash returns the stored translation without prompting`() = runTest {
        val description = "A legendary RPG."
        coEvery { translationDao.getTranslation(1, "it") } returns TranslatedDescriptionEntity(
            gameId = 1,
            languageTag = "it",
            sourceHash = description.hashCode(),
            translatedText = "Un GDR leggendario."
        )

        val result = translator.translate(1, description)

        assertEquals("Un GDR leggendario.", result)
        coVerify(exactly = 0) { geminiNanoClient.generate(any()) }
    }

    @Test
    fun `stale cached hash re-prompts instead of returning the stored translation`() = runTest {
        val description = "A legendary RPG, updated."
        coEvery { translationDao.getTranslation(1, "it") } returns TranslatedDescriptionEntity(
            gameId = 1,
            languageTag = "it",
            sourceHash = "old description".hashCode(),
            translatedText = "Un vecchio testo."
        )
        coEvery { geminiNanoClient.generate(any()) } returns "Un GDR leggendario, aggiornato."

        val result = translator.translate(1, description)

        assertEquals("Un GDR leggendario, aggiornato.", result)
    }

    @Test
    fun `a successful translation is persisted keyed by game id and language`() = runTest {
        val description = "A legendary RPG."
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns "Un GDR leggendario."

        translator.translate(1, description)

        coVerify {
            translationDao.saveTranslation(
                TranslatedDescriptionEntity(
                    gameId = 1,
                    languageTag = "it",
                    sourceHash = description.hashCode(),
                    translatedText = "Un GDR leggendario."
                )
            )
        }
    }

    @Test
    fun `a null result from the client yields null and writes nothing`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns null

        val result = translator.translate(1, "A legendary RPG.")

        assertNull(result)
        coVerify(exactly = 0) { translationDao.saveTranslation(any()) }
    }

    @Test
    fun `a blank description never reaches the client`() = runTest {
        val result = translator.translate(1, "   ")

        assertNull(result)
        coVerify(exactly = 0) { geminiNanoClient.generate(any()) }
        coVerify(exactly = 0) { translationDao.getTranslation(any(), any()) }
    }

    @Test
    fun `an oversized description never reaches the client`() = runTest {
        val result = translator.translate(1, "a".repeat(8001))

        assertNull(result)
        coVerify(exactly = 0) { geminiNanoClient.generate(any()) }
    }

    @Test
    fun `isSupported is false on an English device without asking the client`() = runTest {
        Locale.setDefault(Locale.ENGLISH)

        val result = translator.isSupported()

        assertFalse(result)
        coVerify(exactly = 0) { geminiNanoClient.isModelReady() }
    }

    @Test
    fun `isSupported on a non-English device follows the client's readiness`() = runTest {
        coEvery { geminiNanoClient.isModelReady() } returns true

        val result = translator.isSupported()

        assertTrue(result)
    }
}
