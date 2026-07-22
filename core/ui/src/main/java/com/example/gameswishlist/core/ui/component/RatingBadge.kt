package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.util.ColorUtils

/**
 * Rating badge showing a game's score as both color and icon shape (full/half/outline star),
 * so the score band doesn't rely on color alone to be distinguishable.
 */
@Composable
fun RatingBadge(
    rating: Int,
    modifier: Modifier = Modifier
) {
    val scoreIcon = when {
        rating >= 80 -> Icons.Filled.Star
        rating >= 60 -> Icons.AutoMirrored.Filled.StarHalf
        else -> Icons.Outlined.StarBorder
    }
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(MaterialTheme.spacing.small),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Icon(
                imageVector = scoreIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = ColorUtils.getScoreColor(rating)
            )
            Text(
                text = rating.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingBadgeGoodPreview() {
    GamesWishlistTheme {
        RatingBadge(rating = 92)
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingBadgeMixedPreview() {
    GamesWishlistTheme {
        RatingBadge(rating = 68)
    }
}

@Preview(showBackground = true)
@Composable
private fun RatingBadgePoorPreview() {
    GamesWishlistTheme {
        RatingBadge(rating = 41)
    }
}
