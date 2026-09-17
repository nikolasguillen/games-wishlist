package com.example.gameswishlist.core.data.translation

import com.example.gameswishlist.core.ai.GeminiNanoClient
import com.example.gameswishlist.core.ai.GeminiNanoStatus
import com.example.gameswishlist.core.database.dao.TranslationDao
import com.example.gameswishlist.core.database.entity.TranslatedDescriptionEntity
import com.example.gameswishlist.core.model.TranslationModelStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Covers [GameDescriptionTranslatorImpl]: the cache short-circuit in [GameDescriptionTranslatorImpl.translate],
 * the length/blank guard before Gemini Nano is ever called, and the two-part check in
 * [GameDescriptionTranslatorImpl.modelStatus].
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
    fun `a leading Description label is stripped from a fresh translation`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns "Description:\nUn GDR leggendario."

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `a leading localized Descrizione label is stripped from a fresh translation`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns "Descrizione: Un GDR leggendario."

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `a markdown code fence around the translation is stripped`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns "```\nUn GDR leggendario.\n```"

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `enclosing quotes around the translation are removed`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns "\"Un GDR leggendario.\""

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `a translation containing a colon mid-sentence is left untouched`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns "Capitolo 1: L'inizio."

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Capitolo 1: L'inizio.", result)
    }

    @Test
    fun `the sanitized text, not the raw model output, is what gets persisted`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any()) } returns "Description:\nUn GDR leggendario."

        translator.translate(1, "A legendary RPG.")

        coVerify {
            translationDao.saveTranslation(
                TranslatedDescriptionEntity(
                    gameId = 1,
                    languageTag = "it",
                    sourceHash = "A legendary RPG.".hashCode(),
                    translatedText = "Un GDR leggendario."
                )
            )
        }
    }

    @Test
    fun `a cached row still carrying a leaked label is sanitized on read`() = runTest {
        val description = "A legendary RPG."
        coEvery { translationDao.getTranslation(1, "it") } returns TranslatedDescriptionEntity(
            gameId = 1,
            languageTag = "it",
            sourceHash = description.hashCode(),
            translatedText = "Description:\nUn GDR leggendario."
        )

        val result = translator.translate(1, description)

        assertEquals("Un GDR leggendario.", result)
        coVerify(exactly = 0) { geminiNanoClient.generate(any()) }
    }

    @Test
    fun `modelStatus is UNSUPPORTED on an English device without asking the client`() = runTest {
        Locale.setDefault(Locale.ENGLISH)

        val result = translator.modelStatus()

        assertEquals(TranslationModelStatus.UNSUPPORTED, result)
        coVerify(exactly = 0) { geminiNanoClient.status() }
    }

    @Test
    fun `modelStatus maps an AVAILABLE client status to READY`() = runTest {
        coEvery { geminiNanoClient.status() } returns GeminiNanoStatus.AVAILABLE

        assertEquals(TranslationModelStatus.READY, translator.modelStatus())
    }

    @Test
    fun `modelStatus maps a DOWNLOADING client status to DOWNLOADING`() = runTest {
        coEvery { geminiNanoClient.status() } returns GeminiNanoStatus.DOWNLOADING

        assertEquals(TranslationModelStatus.DOWNLOADING, translator.modelStatus())
    }

    @Test
    fun `modelStatus maps a DOWNLOADABLE client status to DOWNLOADABLE`() = runTest {
        coEvery { geminiNanoClient.status() } returns GeminiNanoStatus.DOWNLOADABLE

        assertEquals(TranslationModelStatus.DOWNLOADABLE, translator.modelStatus())
    }

    @Test
    fun `modelStatus maps an UNAVAILABLE client status to UNSUPPORTED`() = runTest {
        coEvery { geminiNanoClient.status() } returns GeminiNanoStatus.UNAVAILABLE

        assertEquals(TranslationModelStatus.UNSUPPORTED, translator.modelStatus())
    }
}
