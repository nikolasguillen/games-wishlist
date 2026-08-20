package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.util.shimmerEffect
import com.example.gameswishlist.feature.search.R
import com.example.gameswishlist.feature.search.model.GameSuggestionUiModel
import com.example.gameswishlist.core.ui.R as CoreUiR

/** Covers are portrait: a square crop would cut the art the user recognises the game by. */
private val SUGGESTION_COVER_WIDTH = 36.dp
private val SUGGESTION_COVER_HEIGHT = 48.dp

/** Wide enough to read as a section marker rather than as another suggestion. */
private val SECTION_HEADER_LETTER_SPACING = 1.sp

/** A recent query is one line of text, which on its own falls short of the 48dp Material asks for. */
private val MIN_TOUCH_TARGET = 48.dp

/** Smaller than the default 24dp: the history mark supports the query, it does not headline the row. */
private val HISTORY_ICON_SIZE = 20.dp

@Composable
internal fun SuggestionSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = SECTION_HEADER_LETTER_SPACING,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(
            horizontal = MaterialTheme.spacing.large,
            vertical = MaterialTheme.spacing.small
        )
    )
}

/**
 * Commits the typed query, and says so: the filled pill and the chevron break the pattern of the rows
 * above it on purpose, marking the dropdown as a shortcut and the results grid as the complete view.
 */
@Composable
internal fun SeeAllResultsRow(
    query: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.spacing.large,
                vertical = MaterialTheme.spacing.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable { onClick(query) }
            .padding(
                horizontal = MaterialTheme.spacing.mediumLarge,
                vertical = MaterialTheme.spacing.mediumLarge
            )
    ) {
        Text(
            text = stringResource(R.string.see_all_results_for, query),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(MaterialTheme.spacing.mediumLarge)
        )
    }
}

/**
 * A past query: the history icon marks it as something the user typed before, and the muted text keeps
 * it from competing with the cover art of [GameSuggestionRow] right below.
 */
@Composable
internal fun HistorySuggestionRow(
    query: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(query) }
            .heightIn(min = MIN_TOUCH_TARGET)
            .padding(horizontal = MaterialTheme.spacing.large)
    ) {
        // Centred in the width a cover would take, so the queries and the game titles below them
        // start on the same vertical line.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(SUGGESTION_COVER_WIDTH)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(HISTORY_ICON_SIZE)
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.mediumLarge))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
internal fun GameSuggestionRow(
    suggestion: GameSuggestionUiModel,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(suggestion.id) }
            .padding(
                horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.small
            )
    ) {
        AsyncImage(
            model = suggestion.coverUrl,
            contentDescription = null,
            error = painterResource(CoreUiR.drawable.placeholder),
            modifier = Modifier
                .width(SUGGESTION_COVER_WIDTH)
                .height(SUGGESTION_COVER_HEIGHT)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.mediumLarge))
        Column {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (suggestion.subtitle.isNotEmpty()) {
                Text(
                    text = suggestion.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun LoadingSuggestionRow(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.small
            )
    ) {
        Box(
            modifier = Modifier
                .width(SUGGESTION_COVER_WIDTH)
                .height(SUGGESTION_COVER_HEIGHT)
                .clip(MaterialTheme.shapes.small)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.mediumLarge))
        Column {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(16.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(12.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .shimmerEffect()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionSectionHeaderPreview() {
    GamesWishlistTheme {
        Surface {
            SuggestionSectionHeader(title = stringResource(R.string.suggestions_section_recent))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorySuggestionRowPreview() {
    GamesWishlistTheme {
        Surface {
            HistorySuggestionRow(
                query = "The Witcher 3", onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameSuggestionRowPreview() {
    GamesWishlistTheme {
        Surface {
            GameSuggestionRow(
                suggestion = GameSuggestionUiModel(
                    id = 1,
                    name = "The Witcher 3",
                    coverUrl = null,
                    subtitle = "CD Projekt Red · 2015"
                ),
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SeeAllResultsRowPreview() {
    GamesWishlistTheme {
        Surface {
            SeeAllResultsRow(
                query = "Elden Ring",
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingSuggestionRowPreview() {
    GamesWishlistTheme {
        Surface {
            LoadingSuggestionRow()
        }
    }
}
