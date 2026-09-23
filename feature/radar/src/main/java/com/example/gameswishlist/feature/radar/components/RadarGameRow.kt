package com.example.gameswishlist.feature.radar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.CustomInfoChip
import com.example.gameswishlist.core.ui.component.GameListRow
import com.example.gameswishlist.core.ui.component.PlatformTile
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.radar.model.RadarEntryUiModel
import com.example.gameswishlist.feature.radar.model.RadarEntryUiModel.DateLabelStyle

@Composable
internal fun RadarGameRow(
    entry: RadarEntryUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameListRow(
        coverImage = entry.coverImage,
        title = entry.title,
        subtitle = entry.studio,
        onClick = onClick,
        modifier = modifier
    ) {
        // Not hoisted into GameListRow: WishlistGameRow's trailing gap is conditional (only when a rating
        // exists), so a shared gap would leave a dangling Spacer on the games without one.
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        RadarDateLabel(entry)
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.smallMedium))
        PlatformTile(code = entry.platform.code, color = entry.platform.color)
    }
}

@Composable
private fun RadarDateLabel(entry: RadarEntryUiModel) {
    Box(contentAlignment = Alignment.CenterEnd) {
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
