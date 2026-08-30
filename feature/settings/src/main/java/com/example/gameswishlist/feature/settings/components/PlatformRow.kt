package com.example.gameswishlist.feature.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.feature.settings.model.PlatformUiModel

/**
 * One selectable platform. The whole row is the target rather than the checkbox alone, and it carries
 * the `Checkbox` role so the toggle is announced once instead of twice.
 */
@Composable
internal fun PlatformRow(
    platform: PlatformUiModel,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = platform.isSelected,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .padding(
                horizontal = MaterialTheme.spacing.large,
                vertical = MaterialTheme.spacing.medium
            )
    ) {
        Checkbox(checked = platform.isSelected, onCheckedChange = null)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = platform.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            platform.abbreviation?.let { abbreviation ->
                Text(
                    text = abbreviation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlatformRowSelectedPreview() {
    GamesWishlistTheme {
        PlatformRow(
            platform = PlatformUiModel(
                id = 167,
                name = "PlayStation 5",
                abbreviation = "PS5",
                isSelected = true
            ),
            onToggle = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlatformRowUnselectedPreview() {
    GamesWishlistTheme {
        PlatformRow(
            platform = PlatformUiModel(
                id = 471,
                name = "Meta Quest 3",
                abbreviation = null,
                isSelected = false
            ),
            onToggle = {}
        )
    }
}
