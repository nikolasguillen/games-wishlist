package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing

/**
 * The band at the top of a detail page that is not a game: a [cover], then the [title] with an optional
 * [description] and a row of small [chips] beside it. The wishlist's detail screen and the character and developer
 * pages draw it, so the three look alike.
 *
 * @param cover The image slot, drawn at the start. It brings its own size (see [DetailHeaderDefaults]) and shape.
 * @param titleMaxLines How many lines the title may use before it is cut with an ellipsis.
 * @param chips The facts shown under the text, laid out in a row.
 */
@Composable
fun DetailHeader(
    title: String,
    cover: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    titleMaxLines: Int = 1,
    chips: @Composable RowScope.() -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(MaterialTheme.spacing.large)
    ) {
        cover()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.small)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                modifier = Modifier.padding(top = MaterialTheme.spacing.mediumLarge),
                content = chips
            )
        }
    }
}

/** The size of the cover a [DetailHeader] is designed around, and of the icon that stands in for a missing image. */
object DetailHeaderDefaults {
    val CoverWidth: Dp = 96.dp
    val CoverHeight: Dp = 128.dp
    val PlaceholderIconSize: Dp = 40.dp
}

@QuestLogPreviews
@Composable
private fun DetailHeaderPreview() {
    QuestLogTheme {
        DetailHeader(
            title = "Couch Co-op",
            description = "Games worth playing together, controllers in hand.",
            cover = {},
            chips = {
                CustomInfoChip(
                    text = "12 games",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isLarge = false
                )
            }
        )
    }
}

@QuestLogPreviews
@Composable
private fun DetailHeaderTwoLineTitlePreview() {
    QuestLogTheme {
        DetailHeader(
            title = "A title long enough that it needs a second line to fit beside the cover",
            titleMaxLines = 2,
            cover = {}
        )
    }
}
