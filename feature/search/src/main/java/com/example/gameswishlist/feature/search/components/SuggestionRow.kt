package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.util.shimmerEffect
import com.example.gameswishlist.feature.search.model.GameSuggestionUiModel
import com.example.gameswishlist.feature.search.R as SearchR

@Composable
fun SearchActionRow(
    query: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(query) }
            .padding(MaterialTheme.spacing.large)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Text(
            text = buildAnnotatedString {
                append(stringResource(SearchR.string.search_for_action_prefix))
                append(" ")
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    append("\"$query\"")
                }
            },
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HistorySuggestionRow(
    query: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(query) }
            .padding(MaterialTheme.spacing.large)) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Text(
            text = query, color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun GameSuggestionRow(
    suggestion: GameSuggestionUiModel,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(suggestion.id) }
            .padding(
                horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium
            )
    ) {
        AsyncImage(
            model = suggestion.coverUrl,
            contentDescription = null,
            error = painterResource(R.drawable.placeholder),
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
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
fun LoadingSuggestionRow(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.spacing.large, vertical = MaterialTheme.spacing.medium
            )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
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
private fun SearchActionRowPreview() {
    GamesWishlistTheme {
        Surface {
            SearchActionRow(
                query = "Elden Ring",
                onClick = {}
            )
        }
    }
}
