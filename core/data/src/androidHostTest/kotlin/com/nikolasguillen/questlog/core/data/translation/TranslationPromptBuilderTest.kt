package com.nikolasguillen.questlog.core.data.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The prompt both translators share. The instructions are the fixed half and the source the variable one, so the
 * instructions must never carry the `<text>` tag themselves: the source supplies it.
 */
class TranslationPromptBuilderTest {

    @Test
    fun `the instructions name the target language`() {
        assertTrue("into Italian." in buildTranslationInstructions("Italian"))
    }

    @Test
    fun `the instructions refer to the text tags without containing one`() {
        val instructions = buildTranslationInstructions("Italian")

        assertTrue("between the <text> tags" in instructions)
        assertFalse(instructions.lines().any { it.trim() == "<text>" })
        assertFalse("</text>" in instructions)
    }

    @Test
    fun `the source wraps the description in text tags`() {
        assertEquals("<text>\nA legendary RPG.\n</text>", buildTranslationSource("A legendary RPG."))
    }
}
