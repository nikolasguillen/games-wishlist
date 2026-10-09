package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.database.dao.TranslationDao
import com.nikolasguillen.questlog.core.database.entity.TranslatedDescriptionEntity
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [AppleLanguageModelTranslator] against a fake bridge: how Apple's availability folds into
 * [TranslationModelStatus], the prompt it hands the bridge, the cancellation path into Swift, and the polling
 * that stands in for a download the app cannot start.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppleLanguageModelTranslatorTest {

    private val bridge = FakeAppleLanguageModelBridge()
    private val translationDao = mockk<TranslationDao>(relaxed = true)

    private val translator = AppleLanguageModelTranslator(bridge, translationDao)

    @Test
    fun `an English preferred language is unsupported without asking the model`() = runTest {
        bridge.languageTag = "en-US"

        assertEquals(TranslationModelStatus.UNSUPPORTED, translator.modelStatus())
        assertEquals(0, bridge.availabilityCalls)
    }

    @Test
    fun `an available model is ready`() = runTest {
        bridge.availability = AppleLanguageModelAvailability.AVAILABLE

        assertEquals(TranslationModelStatus.READY, translator.modelStatus())
    }

    @Test
    fun `a model the system is still preparing is downloading`() = runTest {
        bridge.availability = AppleLanguageModelAvailability.NOT_READY

        assertEquals(TranslationModelStatus.DOWNLOADING, translator.modelStatus())
    }

    @Test
    fun `an unavailable model is unsupported`() = runTest {
        bridge.availability = AppleLanguageModelAvailability.UNAVAILABLE

        assertEquals(TranslationModelStatus.UNSUPPORTED, translator.modelStatus())
    }

    @Test
    fun `a language the model cannot write is unsupported`() = runTest {
        bridge.availability = AppleLanguageModelAvailability.LANGUAGE_UNSUPPORTED

        assertEquals(TranslationModelStatus.UNSUPPORTED, translator.modelStatus())
    }

    @Test
    fun `the model gets the rules as instructions and the wrapped description as the prompt`() = runTest {
        coEvery { translationDao.getTranslation(1, "it-IT") } returns null

        translator.translate(1, "A legendary RPG.")

        val request = bridge.requests.single()
        assertTrue("into Italian." in request.instructions)
        assertFalse("<text>\n" in request.instructions)
        assertEquals("<text>\nA legendary RPG.\n</text>", request.prompt)
    }

    @Test
    fun `the sanitized result is saved under the game and the bridge's language tag`() = runTest {
        coEvery { translationDao.getTranslation(1, "it-IT") } returns null
        bridge.result = "Descrizione: Un GDR leggendario."

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
        coVerify {
            translationDao.saveTranslation(
                TranslatedDescriptionEntity(
                    gameId = 1,
                    languageTag = "it-IT",
                    sourceHash = "A legendary RPG.".hashCode(),
                    translatedText = "Un GDR leggendario."
                )
            )
        }
    }

    @Test
    fun `a null result yields null and writes nothing`() = runTest {
        coEvery { translationDao.getTranslation(1, "it-IT") } returns null
        bridge.result = null

        assertNull(translator.translate(1, "A legendary RPG."))
        coVerify(exactly = 0) { translationDao.saveTranslation(any()) }
    }

    @Test
    fun `a cache hit with a matching hash never reaches the model`() = runTest {
        val description = "A legendary RPG."
        coEvery { translationDao.getTranslation(1, "it-IT") } returns TranslatedDescriptionEntity(
            gameId = 1,
            languageTag = "it-IT",
            sourceHash = description.hashCode(),
            translatedText = "Un GDR leggendario."
        )

        assertEquals("Un GDR leggendario.", translator.translate(1, description))
        assertTrue(bridge.requests.isEmpty())
    }

    @Test
    fun `a blank or oversized description never reaches the model`() = runTest {
        assertNull(translator.translate(1, "   "))
        assertNull(translator.translate(1, "a".repeat(MAX_TRANSLATABLE_CHARS + 1)))
        assertTrue(bridge.requests.isEmpty())
    }

    @Test
    fun `cancelling the caller cancels the generation running in Swift`() = runTest {
        coEvery { translationDao.getTranslation(1, "it-IT") } returns null
        bridge.respond = false

        val job = launch { translator.translate(1, "A legendary RPG.") }
        runCurrent()
        assertFalse(bridge.cancelled)

        job.cancel()
        advanceUntilIdle()

        assertTrue(bridge.cancelled)
    }

    @Test
    fun `downloadModel starts indeterminate and completes once the model is available`() = runTest {
        bridge.availabilitySequence += AppleLanguageModelAvailability.NOT_READY
        bridge.availabilitySequence += AppleLanguageModelAvailability.AVAILABLE

        val emissions = translator.downloadModel().toList()

        assertEquals(
            listOf(TranslationModelDownload.InProgress(fraction = null), TranslationModelDownload.Completed),
            emissions
        )
        assertEquals(2 * 15_000L, testScheduler.currentTime)
    }

    @Test
    fun `downloadModel fails when the model becomes unavailable`() = runTest {
        bridge.availability = AppleLanguageModelAvailability.UNAVAILABLE

        val emissions = translator.downloadModel().toList()

        assertEquals(TranslationModelDownload.Failed, emissions.last())
    }
}
