package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.PlatformTileUiModel
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel
import com.example.gameswishlist.feature.gamedetail.model.ReleaseInfoUiModel

/**
 * A section displaying detailed game information like description and ratings.
 */
@Composable
fun GameDetailInfoSection(
    description: UiText,
    rating: RatingUiModel?,
    releaseInfo: ReleaseInfoUiModel,
    platforms: List<PlatformTileUiModel>,
    modifier: Modifier = Modifier
) {
    val descriptionString = description.asString()

    Column(modifier = modifier) {
        // Rating Card
        if (rating != null) {
            GameRatingCard(rating = rating)
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Release Info Card (also carries the platform tile strip)
        GameReleaseInfoCard(releaseInfo = releaseInfo, platforms = platforms)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

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
                PlatformTileUiModel(id = 1, code = UiText.DynamicString("PC"), color = Color(0xFF5E5E5E)),
                PlatformTileUiModel(id = 2, code = UiText.DynamicString("PS5"), color = Color(0xFF2E4EA6)),
                PlatformTileUiModel(id = 3, code = UiText.DynamicString("XSX"), color = Color(0xFF107C10))
            )
        )
    }
}
