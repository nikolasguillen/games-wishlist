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

/**
 * A section displaying detailed game information like description and platforms.
 */
@Composable
fun GameDetailInfoSection(
    description: String,
    platforms: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Description
        Text(
            text = stringResource(R.string.description_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraLarge))

        // Platforms
        Text(
            text = stringResource(R.string.platforms_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
        Text(
            text = platforms.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailInfoSectionPreview() {
    MaterialTheme {
        GameDetailInfoSection(
            description = "An epic adventure in a vast open world.",
            platforms = listOf("PC", "PS5", "Xbox Series X")
        )
    }
}
