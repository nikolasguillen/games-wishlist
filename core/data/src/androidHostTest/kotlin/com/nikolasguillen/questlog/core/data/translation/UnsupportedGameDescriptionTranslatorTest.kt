package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The translator a platform without an on-device model binds. Settings and Game detail already hide their
 * translation entry points on `UNSUPPORTED`, so nothing else should ever reach it - but if something does, it
 * must answer the way the Android implementation does on an unsupported device, never throw.
 */
class UnsupportedGameDescriptionTranslatorTest {

    private val translator = UnsupportedGameDescriptionTranslator()

    @Test
    fun `the model is always reported as unsupported`() = runTest {
        assertEquals(TranslationModelStatus.UNSUPPORTED, translator.modelStatus())
    }

    @Test
    fun `translating returns null, which the caller reads as show the original text`() = runTest {
        assertNull(translator.translate(gameId = 1, description = "A long enough description"))
    }

    @Test
    fun `asking for a download reports a failure instead of starting one`() = runTest {
        assertEquals(TranslationModelDownload.Failed, translator.downloadModel().first())
    }
}
