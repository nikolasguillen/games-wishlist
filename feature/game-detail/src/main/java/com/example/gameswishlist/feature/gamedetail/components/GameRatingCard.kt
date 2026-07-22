package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.ColorUtils
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel

/**
 * A card displaying a unified row of rating metrics for a game (Metascore, hypes, ratings count).
 * Only the score metric carries a tier icon (full/half/outline star) alongside its color.
 */
@Composable
internal fun GameRatingCard(
    rating: RatingUiModel,
    modifier: Modifier = Modifier
) {
    CustomContentCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Metascore
            RatingStatItem(
                label = rating.scoreLabel.asString(),
                value = rating.scoreText.asString(),
                score = rating.score,
                modifier = Modifier.weight(1f)
            )

            // Hypes
            if (rating.hypes != null && rating.hypesLabel != null) {
                VerticalDivider(modifier = Modifier.height(40.dp))
                RatingStatItem(
                    label = rating.hypesLabel.asString(),
                    value = rating.hypes.asString(),
                    icon = Icons.Default.Whatshot,
                    iconColor = MaterialTheme.appColors.hypeColor,
                    modifier = Modifier.weight(1f)
                )
            }

            // Rating Count
            if (rating.ratingCount != null && rating.ratingCountLabel != null) {
                VerticalDivider(modifier = Modifier.height(40.dp))
                RatingStatItem(
                    label = rating.ratingCountLabel.asString(),
                    value = rating.ratingCount.asString(),
                    icon = Icons.Default.Star,
                    iconColor = MaterialTheme.appColors.ratingCountColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RatingStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    score: Int? = null,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    val resolvedIcon = if (score != null) Icons.Default.Speed else icon
    val resolvedIconColor = if (score != null) ColorUtils.getScoreColor(score) else iconColor

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (resolvedIcon != null) {
            Icon(
                imageVector = resolvedIcon,
                contentDescription = null,
                tint = resolvedIconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameRatingCardPreview() {
    GamesWishlistTheme {
        GameRatingCard(
            rating = RatingUiModel(
                score = 95,
                scoreText = UiText.DynamicString("95"),
                scoreLabel = UiText.DynamicString("Metascore"),
                hypes = UiText.DynamicString("1.2K"),
                hypesLabel = UiText.DynamicString("Hypes"),
                ratingCount = UiText.DynamicString("450"),
                ratingCountLabel = UiText.DynamicString("Ratings")
            )
        )
    }
}
