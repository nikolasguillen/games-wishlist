package com.example.gameswishlist.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.feature.search.model.SearchSuggestionUiModel

@Composable
fun SuggestionRow(
    suggestion: SearchSuggestionUiModel,
    onClick: (SearchSuggestionUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    when (suggestion) {
        is SearchSuggestionUiModel.History -> HistorySuggestionRow(suggestion, onClick, modifier)
        is SearchSuggestionUiModel.Game -> GameSuggestionRow(suggestion, onClick, modifier)
    }
}

@Composable
private fun HistorySuggestionRow(
    suggestion: SearchSuggestionUiModel.History,
    onClick: (SearchSuggestionUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(suggestion) }
            .padding(MaterialTheme.spacing.large)) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
        Text(
            text = suggestion.query, color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GameSuggestionRow(
    suggestion: SearchSuggestionUiModel.Game,
    onClick: (SearchSuggestionUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onClick(suggestion) }
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

@Preview(showBackground = true)
@Composable
private fun SuggestionRowPreview() {
    GamesWishlistTheme {
        Surface {
            SuggestionRow(
                suggestion = SearchSuggestionUiModel.History("The Witcher 3"), onClick = {})
        }
    }
}
