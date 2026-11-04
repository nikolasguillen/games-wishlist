package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.discover_dismiss_refresh_prompt
import com.nikolasguillen.questlog.feature.search.resources.discover_refresh_prompt_subtitle
import com.nikolasguillen.questlog.feature.search.resources.discover_refresh_prompt_title
import org.jetbrains.compose.resources.stringResource

/**
 * A full-width bar, not a chip: [androidx.compose.material3.AssistChip] is fixed at Material's own 32.dp height, which leaves no
 * room to make the prompt read as more than a minor annotation, and an overlay needs to span the
 * list's width anyway so nothing scrolling underneath shows through beside it. This is
 * hand-rolled so it can be sized and elevated to actually separate from the shelf scrolling under it.
 *
 * Shared by the compact and the wide Discover feeds, which both draw it in the same top overlay.
 */
@Composable
internal fun DiscoverRefreshPrompt(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = !isRefreshing,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = MaterialTheme.spacing.medium,
        shadowElevation = MaterialTheme.spacing.medium,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.large,
                end = MaterialTheme.spacing.small,
                top = MaterialTheme.spacing.medium,
                bottom = MaterialTheme.spacing.medium
            )
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.discover_refresh_prompt_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(Res.string.discover_refresh_prompt_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                )
            }

            IconButton(onClick = onDismissClick, enabled = !isRefreshing) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.discover_dismiss_refresh_prompt)
                )
            }
        }
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverRefreshPromptPreview() {
    QuestLogTheme {
        DiscoverRefreshPrompt(
            isRefreshing = false,
            onClick = {},
            onDismissClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}

@QuestLogPreviews
@Composable
private fun DiscoverRefreshPromptRefreshingPreview() {
    QuestLogTheme {
        DiscoverRefreshPrompt(
            isRefreshing = true,
            onClick = {},
            onDismissClick = {},
            modifier = Modifier.padding(MaterialTheme.spacing.large)
        )
    }
}
