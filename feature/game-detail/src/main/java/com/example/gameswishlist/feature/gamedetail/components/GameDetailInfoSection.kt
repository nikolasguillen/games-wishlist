package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomInfoChip
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel
import com.example.gameswishlist.feature.gamedetail.model.ReleaseInfoUiModel

/**
 * A section displaying detailed game information like description and ratings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDetailInfoSection(
    description: UiText,
    rating: RatingUiModel?,
    releaseInfo: ReleaseInfoUiModel?,
    platforms: List<UiText>,
    modifier: Modifier = Modifier
) {
    val descriptionString = description.asString()

    Column(modifier = modifier) {
        // Rating Card
        if (rating != null) {
            GameRatingCard(rating = rating)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Release Info Card
        if (releaseInfo != null) {
            GameReleaseInfoCard(releaseInfo = releaseInfo)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Platforms
        if (platforms.isNotEmpty()) {
            GamePlatformsFlowRow(platforms = platforms)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Description
        if (descriptionString.isNotEmpty()) {
            GameDescriptionCard(
                description = descriptionString,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GamePlatformsFlowRow(
    platforms: List<UiText>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        platforms.forEach { platform ->
            CustomInfoChip(text = platform.asString())
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailInfoSectionPreview() {
    GamesWishlistTheme {
        GameDetailInfoSection(
            description = UiText.DynamicString("An epic adventure in a vast open world."),
            rating = RatingUiModel(
                score = 95,
                scoreText = UiText.DynamicString("95"),
                scoreLabel = UiText.DynamicString("Metascore"),
                hypes = UiText.DynamicString("120"),
                hypesLabel = UiText.DynamicString("Hypes"),
                ratingCount = UiText.DynamicString("450"),
                ratingCountLabel = UiText.DynamicString("Ratings")
            ),
            releaseInfo = ReleaseInfoUiModel(
                mainDate = UiText.DynamicString("May 20th, 2026"),
                detailedMessage = UiText.DynamicString("May 20th, 2026\nMay 20th, 2026\nMay 20th, 2026"),
                isExpandable = true
            ),
            platforms = listOf(
                UiText.DynamicString("PC"),
                UiText.DynamicString("PS5"),
                UiText.DynamicString("Xbox Series X")
            )
        )
    }
}
