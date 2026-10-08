package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nikolasguillen.questlog.core.designsystem.theme.appColors

object ColorUtils {
    /**
     * Returns a color representing a score (0-100), resolved from the active theme so each tier keeps
     * its contrast in both light and dark.
     * Green for >= 80, Yellow for >= 60, Red for < 60.
     */
    @Composable
    @ReadOnlyComposable
    fun getScoreColor(score: Int): Color {
        val colors = MaterialTheme.appColors
        return when {
            score >= 80 -> colors.ratingHighColor
            score >= 60 -> colors.ratingMidColor
            else -> colors.ratingLowColor
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
