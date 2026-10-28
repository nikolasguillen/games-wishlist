package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.domain.translation.TranslationSubject
import com.nikolasguillen.questlog.core.model.TranslationModelDownload
import com.nikolasguillen.questlog.core.model.TranslationModelStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnsupportedDescriptionTranslatorTest {

    private val translator = UnsupportedDescriptionTranslator()

    @Test
    fun `the status is unsupported`() = runTest {
        assertEquals(TranslationModelStatus.UNSUPPORTED, translator.modelStatus())
    }

    @Test
    fun `a description is never translated`() = runTest {
        assertNull(translator.translate(subject = TranslationSubject.Game(id = 1), description = "A game about a plumber."))
    }

    @Test
    fun `a download fails at once`() = runTest {
        assertEquals(listOf(TranslationModelDownload.Failed), translator.downloadModel().toList())
    }
}
