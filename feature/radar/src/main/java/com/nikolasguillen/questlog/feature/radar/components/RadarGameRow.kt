package com.nikolasguillen.questlog.feature.radar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.CustomInfoChip
import com.nikolasguillen.questlog.core.ui.component.GameListRow
import com.nikolasguillen.questlog.core.ui.component.PlatformTile
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.radar.model.RadarEntryUiModel
import com.nikolasguillen.questlog.feature.radar.model.RadarEntryUiModel.DateLabelStyle
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

@Composable
internal fun RadarGameRow(
    entry: RadarEntryUiModel,
    onClick: () -> Unit,
    onToggleNotification: () -> Unit,
    modifier: Modifier = Modifier,
    showNotificationToggle: Boolean = true
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
        PlatformTile(code = entry.platform.code, containerColor = entry.platform.color)
        if (showNotificationToggle) {
            RadarNotificationToggle(
                isEnabled = entry.isNotificationEnabled,
                onClick = onToggleNotification
            )
        }
    }
}

@Composable
private fun RadarNotificationToggle(isEnabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isEnabled) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
            contentDescription = stringResource(
                if (isEnabled) {
                    CoreUiR.string.disable_notification_content_description
                } else {
                    CoreUiR.string.enable_notification_content_description
                }
            ),
            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
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

@QuestLogPreviews
@Composable
private fun RadarGameRowThisWeekPreview() {
    QuestLogTheme {
        Surface {
            RadarGameRow(entry = RadarEntryUiModel.getDummy(), onClick = {}, onToggleNotification = {})
        }
    }
}

@QuestLogPreviews
@Composable
private fun RadarGameRowPlainPreview() {
    QuestLogTheme {
        Surface {
            RadarGameRow(
                entry = RadarEntryUiModel.getDummy().copy(
                    dateLabel = UiText.DynamicString("Oct 19"),
                    dateSubLabel = null,
                    dateStyle = DateLabelStyle.PLAIN,
                    isNotificationEnabled = true
                ),
                onClick = {},
                onToggleNotification = {}
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun RadarGameRowPillPreview() {
    QuestLogTheme {
        Surface {
            RadarGameRow(
                entry = RadarEntryUiModel.getDummy().copy(
                    dateLabel = UiText.DynamicString("Q1 2027"),
                    dateSubLabel = null,
                    dateStyle = DateLabelStyle.PILL_ACCENT
                ),
                onClick = {},
                onToggleNotification = {}
            )
        }
    }
}
