package com.example.gameswishlist.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.gamecard.MiniGameCard

/**
 * Shared row shape for a game list: cover, title (+ optional subtitle), then a feature-specific
 * [trailingContent] slot. Radar and Wishlist are the two callers -- their trailing content (date/platform
 * vs. a rating badge) has nothing in common beyond sitting at the end of the same row, so it stays a slot
 * rather than a shared shape.
 */
@Composable
fun GameListRow(
    coverImage: String?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.spacing.large,
                vertical = MaterialTheme.spacing.mediumLarge
            )
    ) {
        MiniGameCard(coverImage = coverImage)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        trailingContent()
    }
}

@Preview(showBackground = true)
@Composable
private fun GameListRowWithSubtitlePreview() {
    GamesWishlistTheme {
        Surface {
            GameListRow(
                coverImage = null,
                title = "Hollow Knight: Silksong",
                subtitle = "Team Cherry",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameListRowNoSubtitlePreview() {
    GamesWishlistTheme {
        Surface {
            GameListRow(
                coverImage = null,
                title = "The Witcher 3: Wild Hunt",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameListRowWithTrailingContentPreview() {
    GamesWishlistTheme {
        Surface {
            GameListRow(
                coverImage = null,
                title = "Elden Ring",
                subtitle = "FromSoftware",
                onClick = {}
            ) {
                Text(
                    text = "96",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
