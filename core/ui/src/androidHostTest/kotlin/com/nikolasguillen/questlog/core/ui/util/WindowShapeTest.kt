package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The app's one landscape rule: a space wider than it is tall is landscape, and a square is portrait.
 */
class WindowShapeTest {

    @Test
    fun `a space wider than it is tall is landscape`() {
        assertTrue(isLandscapeShape(width = 900.dp, height = 600.dp))
    }

    @Test
    fun `a space taller than it is wide is portrait`() {
        assertFalse(isLandscapeShape(width = 580.dp, height = 800.dp))
    }

    @Test
    fun `a square space is portrait`() {
        assertFalse(isLandscapeShape(width = 700.dp, height = 700.dp))
    }
}
