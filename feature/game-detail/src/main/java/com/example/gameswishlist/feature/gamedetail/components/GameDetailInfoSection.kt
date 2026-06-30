package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomContentCard
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.ColorUtils
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel

/**
 * A section displaying detailed game information like description and platforms.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDetailInfoSection(
    description: UiText,
    platforms: UiText,
    engines: UiText, // Engines property kept in signature but hidden in UI for now
    rating: RatingUiModel?,
    releaseDates: List<Pair<UiText, UiText>>,
    modifier: Modifier = Modifier
) {
    val descriptionString = description.asString()
    val platformsString = platforms.asString()

    Column(modifier = modifier) {
        // Description
        if (descriptionString.isNotEmpty()) {
            CustomContentCard(
                title = stringResource(R.string.description_title),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = descriptionString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        }

        // Metascore & Release Dates Row
        if (rating != null || releaseDates.isNotEmpty() || platformsString.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                // Rating Card
                if (rating != null) {
                    CustomContentCard(
                        title = rating.label.asString(),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = MaterialTheme.spacing.small),
                            contentAlignment = Alignment.Center
                        ) {
                            val scoreColor = ColorUtils.getScoreColor(rating.score)

                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { rating.score / 100f },
                                    modifier = Modifier.size(80.dp),
                                    color = scoreColor,
                                    strokeWidth = 8.dp,
                                    trackColor = scoreColor.copy(alpha = 0.1f),
                                    strokeCap = StrokeCap.Round,
                                )
                                Text(
                                    text = rating.score.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Platforms & Release Dates Card
                if (releaseDates.isNotEmpty() || platformsString.isNotEmpty()) {
                    CustomContentCard(
                        title = stringResource(
                            if (releaseDates.isNotEmpty()) R.string.release_dates_title else R.string.platforms_title
                        ),
                        modifier = Modifier
                            .weight(if (rating != null) 2f else 1f)
                            .fillMaxHeight()
                    ) {
                        if (releaseDates.isNotEmpty()) {
                            Column {
                                releaseDates.forEachIndexed { index, (platform, date) ->
                                    Text(
                                        text = "${platform.asString()}: ${date.asString()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (index < releaseDates.lastIndex) {
                                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = platformsString,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailInfoSectionPreview() {
    GamesWishlistTheme {
        GameDetailInfoSection(
            description = UiText.DynamicString("An epic adventure in a vast open world."),
            platforms = UiText.DynamicString("PC, PS5"),
            engines = UiText.DynamicString("Unreal Engine 5"),
            rating = RatingUiModel(95, UiText.DynamicString("Metascore")),
            releaseDates = listOf(
                UiText.DynamicString("PC") to UiText.DynamicString("May 20th, 2026"),
                UiText.DynamicString("PS5") to UiText.DynamicString("May 22nd, 2026")
            )
        )
    }
}
