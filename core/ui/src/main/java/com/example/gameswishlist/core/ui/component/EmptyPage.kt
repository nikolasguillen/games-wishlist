package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A full-screen empty state. [actionLabel] and [onActionClick] travel together -- the action renders
 * only when both are non-null -- because unlike [ErrorPage]'s fixed "Retry", what an empty state can do
 * about itself is caller-specific (clear a search, clear active filters, retry a sync, ...), so the label
 * has to come from the caller too.
 */
@Composable
fun EmptyPage(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: UiText? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                TextButton(onClick = onActionClick) {
                    Text(text = actionLabel.asString())
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPagePreview() {
    GamesWishlistTheme {
        EmptyPage(
            message = "No games found here.",
            icon = Icons.Outlined.SearchOff
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPageWithActionPreview() {
    GamesWishlistTheme {
        EmptyPage(
            message = "No games match the selected filters",
            icon = Icons.Outlined.SearchOff,
            actionLabel = UiText.DynamicString("Clear filters"),
            onActionClick = {}
        )
    }
}
