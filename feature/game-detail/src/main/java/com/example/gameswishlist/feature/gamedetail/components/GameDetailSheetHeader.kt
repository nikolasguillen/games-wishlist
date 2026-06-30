package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.appColors
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomInfoChip
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A header component for the game detail sheet containing the game name and metadata.
 *
 * @param name The name of the game.
 * @param gameType The type of the game (e.g., Main Game, DLC).
 * @param companyInfo The developer(s) and publisher(s) of the game.
 * @param genres The list of game genres.
 * @param modifier The modifier to be applied to the header.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameDetailSheetHeader(
    name: UiText,
    gameType: UiText,
    companyInfo: UiText,
    genres: List<UiText>,
    modifier: Modifier = Modifier
) {
    val subtitle = companyInfo.asString()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = name.asString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            // Game Type Chip
            CustomInfoChip(text = gameType.asString())

            // Genre Chips
            genres.forEach { genre ->
                CustomInfoChip(
                    text = genre.asString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailSheetHeaderPreview() {
    GamesWishlistTheme {
        GameDetailSheetHeader(
            name = UiText.DynamicString("The Witcher 3: Wild Hunt"),
            gameType = UiText.DynamicString("Main Game"),
            companyInfo = UiText.DynamicString("CD Projekt Red, CD Projekt"),
            genres = listOf(
                UiText.DynamicString("RPG"),
                UiText.DynamicString("Open World"),
                UiText.DynamicString("Action")
            ),
            modifier = Modifier.background(MaterialTheme.appColors.appBackground)
        )
    }
}
