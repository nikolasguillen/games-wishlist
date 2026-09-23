package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.model.PlatformTileUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.gamedetail.model.AvailabilityUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.DescriptionTranslationState
import com.nikolasguillen.questlog.feature.gamedetail.model.PlatformReleaseDateUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.RatingUiModel

/**
 * A section displaying detailed game information like description and ratings.
 */
@Composable
internal fun GameDetailInfoSection(
    description: UiText,
    descriptionTranslation: DescriptionTranslationState,
    rating: RatingUiModel?,
    availability: AvailabilityUiModel,
    onTranslateClick: () -> Unit,
    onShowOriginalClick: () -> Unit,
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
        GameReleaseInfoCard(availability = availability)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))

        // Description
        if (descriptionString.isNotEmpty()) {
            GameDescriptionCard(
                description = descriptionString,
                translation = descriptionTranslation,
                onTranslateClick = onTranslateClick,
                onShowOriginalClick = onShowOriginalClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailInfoSectionPreview() {
    QuestLogTheme {
        GameDetailInfoSection(
            description = UiText.DynamicString("An epic adventure in a vast open world."),
            descriptionTranslation = DescriptionTranslationState.Available,
            onTranslateClick = {},
            onShowOriginalClick = {},
            rating = RatingUiModel(
                score = 95,
                scoreText = UiText.DynamicString("95"),
                scoreLabel = UiText.DynamicString("Metascore"),
                hypes = UiText.DynamicString("120"),
                hypesLabel = UiText.DynamicString("Hypes"),
                ratingCount = UiText.DynamicString("450"),
                ratingCountLabel = UiText.DynamicString("Ratings")
            ),
            availability = AvailabilityUiModel(
                mainDate = UiText.DynamicString("May 20th, 2026"),
                platforms = listOf(
                    PlatformTileUiModel(id = 1, code = UiText.DynamicString("PC"), color = Color(0xFF5E5E5E)),
                    PlatformTileUiModel(id = 2, code = UiText.DynamicString("PS5"), color = Color(0xFF2E4EA6)),
                    PlatformTileUiModel(id = 3, code = UiText.DynamicString("XSX"), color = Color(0xFF107C10))
                ),
                detailedDates = listOf(
                    PlatformReleaseDateUiModel(
                        platformId = 1,
                        platformName = UiText.DynamicString("PC (Microsoft Windows)"),
                        code = UiText.DynamicString("PC"),
                        color = Color(0xFF5E5E5E),
                        date = UiText.DynamicString("May 20th, 2026")
                    ),
                    PlatformReleaseDateUiModel(
                        platformId = 2,
                        platformName = UiText.DynamicString("PlayStation 5"),
                        code = UiText.DynamicString("PS5"),
                        color = Color(0xFF2E4EA6),
                        date = UiText.DynamicString("May 20th, 2026")
                    )
                ),
                isExpandable = true
            )
        )
    }
}
