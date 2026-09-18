package com.example.gameswishlist.core.data.translation

import com.example.gameswishlist.core.ai.GeminiNanoClient
import com.example.gameswishlist.core.ai.GeminiNanoDownload
import com.example.gameswishlist.core.ai.GeminiNanoStatus
import com.example.gameswishlist.core.database.dao.TranslationDao
import com.example.gameswishlist.core.database.entity.TranslatedDescriptionEntity
import com.example.gameswishlist.core.model.TranslationModelDownload
import com.example.gameswishlist.core.model.TranslationModelStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Covers [GameDescriptionTranslatorImpl]: the cache short-circuit in [GameDescriptionTranslatorImpl.translate],
 * the length/blank guard before Gemini Nano is ever called, the two-part check in
 * [GameDescriptionTranslatorImpl.modelStatus], and [GameDescriptionTranslatorImpl.downloadModel] — its
 * mapping of the client's emissions, sharing one running download across callers instead of starting a
 * second one, and polling status instead of calling the client again when nothing here is tracking an
 * active download, so a download inherited from a dead process still resolves on its own.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameDescriptionTranslatorImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val geminiNanoClient = mockk<GeminiNanoClient>()
    private val translationDao = mockk<TranslationDao>(relaxed = true)

    private val translator = GameDescriptionTranslatorImpl(geminiNanoClient, translationDao, testScope)

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
        coVerify(exactly = 0) { geminiNanoClient.generate(any(), any()) }
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
        coEvery { geminiNanoClient.generate(any(), any()) } returns "Un GDR leggendario, aggiornato."

        val result = translator.translate(1, description)

        assertEquals("Un GDR leggendario, aggiornato.", result)
    }

    @Test
    fun `a successful translation is persisted keyed by game id and language`() = runTest {
        val description = "A legendary RPG."
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any(), any()) } returns "Un GDR leggendario."

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
        coEvery { geminiNanoClient.generate(any(), any()) } returns null

        val result = translator.translate(1, "A legendary RPG.")

        assertNull(result)
        coVerify(exactly = 0) { translationDao.saveTranslation(any()) }
    }

    @Test
    fun `a blank description never reaches the client`() = runTest {
        val result = translator.translate(1, "   ")

        assertNull(result)
        coVerify(exactly = 0) { geminiNanoClient.generate(any(), any()) }
        coVerify(exactly = 0) { translationDao.getTranslation(any(), any()) }
    }

    @Test
    fun `an oversized description never reaches the client`() = runTest {
        val result = translator.translate(1, "a".repeat(8001))

        assertNull(result)
        coVerify(exactly = 0) { geminiNanoClient.generate(any(), any()) }
    }

    @Test
    fun `a leading Description label is stripped from a fresh translation`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any(), any()) } returns "Description:\nUn GDR leggendario."

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `a leading localized Descrizione label is stripped from a fresh translation`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any(), any()) } returns "Descrizione: Un GDR leggendario."

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `a markdown code fence around the translation is stripped`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any(), any()) } returns "```\nUn GDR leggendario.\n```"

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `enclosing quotes around the translation are removed`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any(), any()) } returns "\"Un GDR leggendario.\""

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Un GDR leggendario.", result)
    }

    @Test
    fun `a translation containing a colon mid-sentence is left untouched`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any(), any()) } returns "Capitolo 1: L'inizio."

        val result = translator.translate(1, "A legendary RPG.")

        assertEquals("Capitolo 1: L'inizio.", result)
    }

    @Test
    fun `the sanitized text, not the raw model output, is what gets persisted`() = runTest {
        coEvery { translationDao.getTranslation(1, "it") } returns null
        coEvery { geminiNanoClient.generate(any(), any()) } returns "Description:\nUn GDR leggendario."

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
        coVerify(exactly = 0) { geminiNanoClient.generate(any(), any()) }
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

    @Test
    fun `downloadModel reflects the client's Progress and terminal emissions`() = runTest(testDispatcher) {
        coEvery { geminiNanoClient.status() } returns GeminiNanoStatus.DOWNLOADABLE
        // yield() between emissions, same as GameDescriptionTranslatorImplTest's sibling in
        // SettingsViewModelTest: it forces a suspension point so the StateFlow's other collector — the
        // one asserting below — gets scheduled in between and observes the intermediate value instead of
        // it being conflated away by the terminal one.
        every { geminiNanoClient.download() } returns flow {
            emit(GeminiNanoDownload.Progress(fraction = 0.5f))
            yield()
            emit(GeminiNanoDownload.Completed)
        }

        val collected = mutableListOf<TranslationModelDownload>()
        val job = launch { translator.downloadModel().collect { collected.add(it) } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(TranslationModelDownload.Completed, collected.last())
        assertEquals(true, collected.contains(TranslationModelDownload.InProgress(fraction = 0.5f)))
    }

    @Test
    fun `a second call to downloadModel while a download is active does not start a second one`() = runTest(testDispatcher) {
        coEvery { geminiNanoClient.status() } returns GeminiNanoStatus.DOWNLOADABLE
        // Never completes on its own, mirroring a download still in flight at the SDK level, so
        // downloadJob is still active when the second caller — e.g. a freshly recreated SettingsViewModel
        // after re-entering Settings — arrives.
        every { geminiNanoClient.download() } returns flow {
            emit(GeminiNanoDownload.Progress(fraction = 0.1f))
            awaitCancellation()
        }

        val firstCollectJob = launch { translator.downloadModel().collect {} }
        runCurrent()

        translator.downloadModel()
        runCurrent()

        verify(exactly = 1) { geminiNanoClient.download() }
        firstCollectJob.cancel()
    }

    @Test
    fun `downloadModel polls status instead of calling the client, then resolves once AVAILABLE`() =
        runTest(testDispatcher) {
            // A status inherited from a download that was running in a previous, now-dead process: this
            // process never called geminiNanoClient.download() itself, so downloadJob is null even though
            // the model is genuinely still downloading. The first value is runDownload()'s own initial
            // check; the rest are consumed one per poll inside pollInheritedDownload().
            coEvery { geminiNanoClient.status() } returnsMany listOf(
                GeminiNanoStatus.DOWNLOADING,
                GeminiNanoStatus.DOWNLOADING,
                GeminiNanoStatus.AVAILABLE
            )

            val result = translator.downloadModel()
            runCurrent() // only the initial check runs before the first delay() suspends the poll loop

            assertEquals(TranslationModelDownload.InProgress(fraction = null), result.first())
            verify(exactly = 0) { geminiNanoClient.download() }

            advanceUntilIdle() // safe: the third status() call resolves and ends the poll loop
            assertEquals(TranslationModelDownload.Completed, result.first())
        }

    @Test
    fun `downloadModel resolves to Failed when the inherited download reverts instead of completing`() =
        runTest(testDispatcher) {
            // The system cancelled or dropped the download instead of finishing it: status goes back to
            // DOWNLOADABLE (or UNAVAILABLE) rather than reaching AVAILABLE.
            coEvery { geminiNanoClient.status() } returnsMany listOf(
                GeminiNanoStatus.DOWNLOADING,
                GeminiNanoStatus.DOWNLOADABLE
            )

            val result = translator.downloadModel()
            advanceUntilIdle()

            assertEquals(TranslationModelDownload.Failed, result.first())
            verify(exactly = 0) { geminiNanoClient.download() }
        }
}
