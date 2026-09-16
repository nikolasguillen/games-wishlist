package com.example.gameswishlist.feature.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing

/**
 * One row inside a [SettingsGroup]: leading icon, title, optional subtitle, and a trailing value, a
 * chevron or a switch. The chevron appears only when [onClick] is set and [checked] is not, so a row
 * that goes nowhere never looks tappable; [checked] non-null renders a [Switch] instead, since a toggle
 * already reads as interactive on its own.
 */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    checked: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = MaterialTheme.spacing.large,
                vertical = MaterialTheme.spacing.mediumLarge
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        when {
            checked != null -> Switch(checked = checked, onCheckedChange = { onClick?.invoke() })
            trailingText != null -> Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null && checked == null) {
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowNavigablePreview() {
    GamesWishlistTheme {
        SettingsRow(
            icon = Icons.Default.SportsEsports,
            title = "Owned platforms",
            subtitle = "PS5, PC, Switch",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowValueOnlyPreview() {
    GamesWishlistTheme {
        SettingsRow(
            icon = Icons.Default.SportsEsports,
            title = "About",
            trailingText = "1.0"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsRowSwitchPreview() {
    GamesWishlistTheme {
        SettingsRow(
            icon = Icons.Outlined.Translate,
            title = "Translate descriptions",
            subtitle = "Runs on-device, powered by Gemini Nano",
            checked = true,
            onClick = {}
        )
    }
}
