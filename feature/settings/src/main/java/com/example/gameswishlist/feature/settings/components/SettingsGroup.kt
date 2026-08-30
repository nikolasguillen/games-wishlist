package com.example.gameswishlist.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing

/**
 * A labelled block of settings rows on a raised surface.
 *
 * `CustomContentCard` from `:core:ui` is deliberately not reused here: it pads its content by
 * `spacing.large` on every side, which would inset the rows and stop their ripple from spanning the
 * card. A settings row has to reach both edges.
 */
@Composable
internal fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.large,
                bottom = MaterialTheme.spacing.medium
            )
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsGroupPreview() {
    GamesWishlistTheme {
        SettingsGroup(
            title = "My game profile",
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        ) {
            Text(
                text = "Rows go here",
                modifier = Modifier.padding(MaterialTheme.spacing.large)
            )
        }
    }
}
