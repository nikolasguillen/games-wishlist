package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.ColorUtils
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel

/**
 * A card displaying various rating metrics for a game, such as Metascore, hypes, and ratings count.
 */
@Composable
internal fun GameRatingCard(
    rating: RatingUiModel,
    modifier: Modifier = Modifier
) {
    CustomContentCard(
        title = stringResource(R.string.rating_title),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Metascore
            RatingMetricItem(
                label = rating.scoreLabel.asString(),
                value = rating.scoreText.asString(),
                score = rating.score,
                modifier = Modifier.weight(1f)
            )

            // Hypes
            if (rating.hypes != null && rating.hypesLabel != null) {
                RatingMetricItem(
                    label = rating.hypesLabel.asString(),
                    value = rating.hypes.asString(),
                    icon = Icons.Default.Whatshot,
                    iconColor = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            // Rating Count
            if (rating.ratingCount != null && rating.ratingCountLabel != null) {
                RatingMetricItem(
                    label = rating.ratingCountLabel.asString(),
                    value = rating.ratingCount.asString(),
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFFB300),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RatingMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    score: Int? = null,
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (score != null) {
            val scoreColor = ColorUtils.getScoreColor(score)
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(48.dp),
                    color = scoreColor,
                    strokeWidth = 4.dp,
                    trackColor = scoreColor.copy(alpha = 0.1f),
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

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
