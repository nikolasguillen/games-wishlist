package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing

/**
 * The layout a character card and a developer card share: a square image at the width of the card, with the name
 * under it. The whole card is one button for assistive technology, announced as [contentDescription].
 *
 * @param image Draws the image; it gets a modifier that makes it fill the card's width as a square.
 */
@Composable
internal fun EntityResultCard(
    name: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    image: @Composable (Modifier) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
    ) {
        image(Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@QuestLogPreviews
@Composable
private fun EntityResultCardPreview() {
    QuestLogTheme {
        EntityResultCard(
            name = "A name that is long enough to wrap onto a second line",
            contentDescription = "Preview, card",
            onClick = {},
            modifier = Modifier.width(160.dp)
        ) { imageModifier ->
            Box(imageModifier.background(MaterialTheme.colorScheme.surfaceContainerHighest))
        }
    }
}
