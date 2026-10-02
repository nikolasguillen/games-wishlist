package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.isDarkTheme
import com.nikolasguillen.questlog.core.ui.component.CustomContentCard
import com.nikolasguillen.questlog.core.ui.component.CustomOutlinedIcon
import com.nikolasguillen.questlog.feature.gamedetail.R
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

@Composable
fun GameDetailNotificationsBanner(
    isNotificationEnabled: Boolean,
    onToggleNotification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = !MaterialTheme.isDarkTheme

    CustomContentCard(
        onClick = onToggleNotification,
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = stringResource(R.string.notification_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.notification_banner_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CustomOutlinedIcon(
                imageVector = if (isNotificationEnabled) {
                    Icons.Filled.Notifications
                } else {
                    Icons.Outlined.NotificationsNone
                },
                contentDescription = stringResource(
                    if (isNotificationEnabled) {
                        CoreUiR.string.disable_notification_content_description
                    } else {
                        CoreUiR.string.enable_notification_content_description
                    }
                ),
                tint = if (isNotificationEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                outlineColor = if (isNotificationEnabled && isLightTheme) MaterialTheme.colorScheme.onSurface else null
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun GameDetailNotificationsBannerPreview() {
    QuestLogTheme {
        GameDetailNotificationsBanner(
            isNotificationEnabled = true,
            onToggleNotification = {}
        )
    }
}