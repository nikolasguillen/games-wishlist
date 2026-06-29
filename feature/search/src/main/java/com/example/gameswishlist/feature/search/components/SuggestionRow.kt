package com.example.gameswishlist.feature.search.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.search.model.SearchSuggestionUiModel
import com.example.gameswishlist.core.ui.R as CoreUiR

@Composable
fun SuggestionRow(suggestion: SearchSuggestionUiModel, modifier: Modifier = Modifier) {
    when (suggestion) {
        is SearchSuggestionUiModel.Game -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
                AsyncImage(
                    model = suggestion.coverUrl,
                    contentDescription = null,
                    placeholder = painterResource(CoreUiR.drawable.placeholder),
                    error = painterResource(CoreUiR.drawable.placeholder),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .padding(MaterialTheme.spacing.medium)
                        .clip(MaterialTheme.shapes.small)
                )
                Column {
                    Text(
                        text = suggestion.text.asString(),
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    suggestion.developer?.let {
                        Text(
                            text = it.asString(),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        is SearchSuggestionUiModel.RecentSearch -> {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(
                    all = MaterialTheme.spacing.medium
                )
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
                Text(
                    text = suggestion.text.asString(), color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionRowGamePreview() {
    GamesWishlistTheme {
        Surface {
            SuggestionRow(
                suggestion = SearchSuggestionUiModel.Game(
                    gameId = 1,
                    text = UiText.DynamicString("The Witcher 3: Wild Hunt"),
                    coverUrl = "https://media.rawg.io/media/games/618/618c49a64e2f469d6107ba9357d812d6.jpg",
                    developer = UiText.DynamicString("CD Projekt RED")
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionRowRecentSearchPreview() {
    GamesWishlistTheme {
        Surface {
            SuggestionRow(
                suggestion = SearchSuggestionUiModel.RecentSearch(
                    text = UiText.DynamicString("Elden Ring")
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SuggestionRowGameErrorPreview() {
    GamesWishlistTheme {
        Surface {
            SuggestionRow(
                suggestion = SearchSuggestionUiModel.Game(
                    gameId = 1,
                    text = UiText.DynamicString("Broken Game URL"),
                    coverUrl = null,
                    developer = UiText.DynamicString("Unknown Developer")
                )
            )
        }
    }
}
