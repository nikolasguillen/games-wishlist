package com.example.gameswishlist.core.ui.util

import androidx.compose.ui.graphics.Color

object ColorUtils {
    /**
     * Returns a color representing a score (0-100).
     * Green for >= 80, Yellow for >= 60, Red for < 60.
     */
    fun getScoreColor(score: Int): Color {
        return when {
            score >= 80 -> Color(0xFF4CAF50) // Green
            score >= 60 -> Color(0xFFFFC107) // Yellow
            else -> Color(0xFFF44336)        // Red
        }
    }
}
