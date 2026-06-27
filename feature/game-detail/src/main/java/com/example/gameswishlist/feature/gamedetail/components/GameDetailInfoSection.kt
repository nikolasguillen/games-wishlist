package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A section displaying detailed game information like description and platforms.
 */
@Composable
fun GameDetailInfoSection(
    description: UiText,
    platforms: UiText,
    developers: UiText,
    publishers: UiText,
    engines: UiText,
    releaseDates: List<Pair<UiText, UiText>>,
    modifier: Modifier = Modifier
) {
    val descriptionString = description.asString()
    val developersString = developers.asString()
    val publishersString = publishers.asString()
    val enginesString = engines.asString()
    val platformsString = platforms.asString()

    Column(modifier = modifier) {
        // Description
        if (descriptionString.isNotEmpty()) {
            InfoSectionTitle(stringResource(R.string.description_title))
            Text(
                text = descriptionString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))
        }

        // Developers
        if (developersString.isNotEmpty()) {
            InfoSectionTitle(stringResource(R.string.developers_title))
            Text(
                text = developersString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Publishers
        if (publishersString.isNotEmpty()) {
            InfoSectionTitle(stringResource(R.string.publishers_title))
            Text(
                text = publishersString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Engines
        if (enginesString.isNotEmpty()) {
            InfoSectionTitle(stringResource(R.string.engines_title))
            Text(
                text = enginesString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Platforms
        if (platformsString.isNotEmpty()) {
            InfoSectionTitle(stringResource(R.string.platforms_title))
            Text(
                text = platformsString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }

        // Release Dates
        if (releaseDates.isNotEmpty()) {
            InfoSectionTitle(stringResource(R.string.release_dates_title))
            releaseDates.forEach { (platform, date) ->
                Text(
                    text = "${platform.asString()}: ${date.asString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
}

@Preview(showBackground = true)
@Composable
private fun GameDetailInfoSectionPreview() {
    MaterialTheme {
        GameDetailInfoSection(
            description = UiText.DynamicString("An epic adventure in a vast open world."),
            platforms = UiText.DynamicString("PC, PS5"),
            developers = UiText.DynamicString("Developer Studio"),
            publishers = UiText.DynamicString("Publisher Inc."),
            engines = UiText.DynamicString("Unreal Engine 5"),
            releaseDates = listOf(
                UiText.DynamicString("PC") to UiText.DynamicString("May 20th, 2026"),
                UiText.DynamicString("PS5") to UiText.DynamicString("May 22nd, 2026")
            )
        )
    }
}
