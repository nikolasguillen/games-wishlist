package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.util.UiConstants

/**
 * A full-screen empty state. [actionLabel] and [onActionClick] travel together -- the action renders
 * only when both are non-null -- because unlike [ErrorPage]'s fixed "Retry", what an empty state can do
 * about itself is caller-specific (clear a search, clear active filters, retry a sync, ...), so the label
 * has to come from the caller too. [subtitle] is optional, second-line explanatory text (e.g. Radar's
 * empty state) rendered de-emphasized below [message]; omitted entirely when null.
 *
 * The content takes at most [UiConstants.EMPTY_PAGE_CONTENT_WIDTH_FRACTION] of the width, so the text wraps
 * into a block rather than touching the screen's edges. The action is coloured with `textOnSurface`,
 * because `primary` is unreadable on the light background.
 */
@Composable
fun EmptyPage(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionLabel: UiText? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(UiConstants.EMPTY_PAGE_CONTENT_WIDTH_FRACTION)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.extraLarge)
                )
            }
            if (actionLabel != null && onActionClick != null) {
                TextButton(
                    onClick = onActionClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.appColors.textOnSurface)
                ) {
                    Text(text = actionLabel.asString())
                }
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun EmptyPagePreview() {
    QuestLogTheme {
        EmptyPage(
            message = "No games found here.",
            icon = Icons.Outlined.SearchOff
        )
    }
}

@QuestLogPreviews
@Composable
private fun EmptyPageWithActionPreview() {
    QuestLogTheme {
        EmptyPage(
            message = "No games match the selected filters",
            icon = Icons.Outlined.SearchOff,
            actionLabel = UiText.DynamicString("Clear filters"),
            onActionClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun EmptyPageLongMessagePreview() {
    QuestLogTheme {
        EmptyPage(
            message = "No platforms cached yet.\nCheck your connection and try again.",
            icon = Icons.Outlined.SearchOff,
            actionLabel = UiText.DynamicString("Retry"),
            onActionClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun EmptyPageWithSubtitlePreview() {
    QuestLogTheme {
        EmptyPage(
            message = "Nothing on the radar yet",
            subtitle = "Save games with a release date and they'll show up here, sorted by when they launch.",
            icon = Icons.Outlined.SearchOff
        )
    }
}
