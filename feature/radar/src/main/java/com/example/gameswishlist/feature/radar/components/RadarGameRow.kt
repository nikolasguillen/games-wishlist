package com.example.gameswishlist.feature.radar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomInfoChip
import com.example.gameswishlist.core.ui.component.PlatformTile
import com.example.gameswishlist.core.ui.component.gamecard.MiniGameCard
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.radar.model.RadarEntryUiModel
import com.example.gameswishlist.feature.radar.model.RadarEntryUiModel.DateLabelStyle

/**
 * Minimum width of the trailing date slot, so every row's date lines up on the same edge regardless of
 * which [DateLabelStyle] it renders -- a two-line "Wed / Sep 24" stack is about as wide as this gets, a
 * bare "2027" is much narrower. `widthIn(min = ...)` instead of a fixed `width` so a longer localized
 * label (or a larger font scale) still has room to grow instead of clipping.
 */
private val DateSlotMinWidth = 64.dp

@Composable
internal fun RadarGameRow(
    entry: RadarEntryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        MiniGameCard(coverImage = entry.coverImage)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            entry.studio?.let { studio ->
                Text(
                    text = studio,
                    style = MaterialTheme.typography.bodyMedium,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        PlatformTile(code = entry.platform.code, color = entry.platform.color)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))
        RadarDateLabel(entry)
    }
}

@Composable
private fun RadarDateLabel(entry: RadarEntryUiModel) {
    Box(modifier = Modifier.widthIn(min = DateSlotMinWidth), contentAlignment = Alignment.CenterEnd) {
        when (entry.dateStyle) {
            DateLabelStyle.THIS_WEEK -> Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.dateLabel.asString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                entry.dateSubLabel?.let {
                    Text(
                        text = it.asString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DateLabelStyle.PLAIN -> Text(
                text = entry.dateLabel.asString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            DateLabelStyle.PILL_ACCENT -> CustomInfoChip(
                text = entry.dateLabel.asString(),
                isLarge = false,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )

            DateLabelStyle.PILL_MUTED -> CustomInfoChip(
                text = entry.dateLabel.asString(),
                isLarge = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarGameRowThisWeekPreview() {
    GamesWishlistTheme {
        Surface {
            RadarGameRow(entry = RadarEntryUiModel.getDummy(), onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarGameRowPlainPreview() {
    GamesWishlistTheme {
        Surface {
            RadarGameRow(
                entry = RadarEntryUiModel.getDummy().copy(
                    dateLabel = UiText.DynamicString("Oct 19"),
                    dateSubLabel = null,
                    dateStyle = DateLabelStyle.PLAIN
                ),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarGameRowPillPreview() {
    GamesWishlistTheme {
        Surface {
            RadarGameRow(
                entry = RadarEntryUiModel.getDummy().copy(
                    dateLabel = UiText.DynamicString("Q1 2027"),
                    dateSubLabel = null,
                    dateStyle = DateLabelStyle.PILL_ACCENT
                ),
                onClick = {}
            )
        }
    }
}
