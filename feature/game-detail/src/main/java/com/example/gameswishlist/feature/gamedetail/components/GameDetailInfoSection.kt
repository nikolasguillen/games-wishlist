package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    mainReleaseDate: UiText,
    releaseDates: List<Pair<UiText, UiText>>,
    modifier: Modifier = Modifier
) {
    val descriptionString = description.asString()
    val platformsString = platforms.asString()
    val mainReleaseDateString = mainReleaseDate.asString()

    Column(modifier = modifier) {
        // Description
        if (descriptionString.isNotEmpty()) {
            GameDescriptionCard(
                description = descriptionString,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        }

        // Metascore & Release Dates Row
        if (rating != null || releaseDates.isNotEmpty() || platformsString.isNotEmpty() || mainReleaseDateString.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                // Rating Card
                if (rating != null) {
                    GameRatingCard(
                        rating = rating,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Platforms & Release Dates Card
                if (releaseDates.isNotEmpty() || platformsString.isNotEmpty() || mainReleaseDateString.isNotEmpty()) {
                    GameReleaseInfoCard(
                        platformsString = platformsString,
                        mainReleaseDateString = mainReleaseDateString,
                        releaseDates = releaseDates,
                        modifier = Modifier
                            .weight(if (rating != null) 2f else 1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun GameDescriptionCard(
    description: String,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var hasOverflow by rememberSaveable { mutableStateOf(false) }

    CustomContentCard(
        title = stringResource(R.string.description_title),
        modifier = modifier
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { textLayoutResult ->
                    hasOverflow = textLayoutResult.hasVisualOverflow
                }
            )

            if (hasOverflow || expanded) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = MaterialTheme.spacing.small)
                ) {
                    Text(
                        text = stringResource(if (expanded) R.string.show_less else R.string.show_more),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GameRatingCard(
    rating: RatingUiModel,
    modifier: Modifier = Modifier
) {
    CustomContentCard(
        title = rating.label.asString(),
        modifier = modifier
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

@Composable
private fun GameReleaseInfoCard(
    platformsString: String,
    mainReleaseDateString: String,
    releaseDates: List<Pair<UiText, UiText>>,
    modifier: Modifier = Modifier
) {
    val title = stringResource(
        if (releaseDates.isNotEmpty() || mainReleaseDateString.isNotEmpty()) R.string.release_dates_title else R.string.platforms_title
    )

    CustomContentCard(
        title = title,
        modifier = modifier
    ) {
        Column {
            if (mainReleaseDateString.isNotEmpty()) {
                Text(
                    text = mainReleaseDateString,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            }

            if (releaseDates.isNotEmpty()) {
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
            } else if (platformsString.isNotEmpty()) {
                Text(
                    text = platformsString,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            mainReleaseDate = UiText.DynamicString("May 20th, 2026"),
            releaseDates = listOf(
                UiText.DynamicString("PC") to UiText.DynamicString("May 20th, 2026"),
                UiText.DynamicString("PS5") to UiText.DynamicString("May 22nd, 2026")
            )
        )
    }
}
