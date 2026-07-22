package com.example.gameswishlist.core.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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

    /**
     * Returns an icon representing a score's tier (full/half/outline star), so the
     * tier doesn't rely on color alone to be distinguishable.
     */
    fun getScoreIcon(score: Int): ImageVector {
        return when {
            score >= 80 -> Icons.Filled.Star
            score >= 60 -> Icons.AutoMirrored.Filled.StarHalf
            else -> Icons.Outlined.StarBorder
        }
    }
}
