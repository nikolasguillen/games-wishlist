package com.example.gameswishlist.feature.wishlist.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.wishlist.model.WishlistSectionUiModel

// sections is always the same instance from WishlistUiState until it actually changes, so the
// instability falls back to reference comparison, which already skips correctly.
@Suppress("ComposeUnstableCollections")
@Composable
internal fun WishlistGamesList(
    sections: List<WishlistSectionUiModel>,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        sections.forEach { section ->
            val label = section.label
            if (label != null) {
                item(key = "header_${section.status}") {
                    StatusSectionHeader(
                        label = label.asString(),
                        count = section.games.size,
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                    )
                }
            }
            itemsIndexed(
                items = section.games,
                key = { _, game -> game.id }
            ) { index, game ->
                WishlistGameRow(
                    game = game,
                    onClick = { onGameClick(game.id) }
                )
                if (index < section.games.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
                    )
                } else {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistGamesListPreview() {
    GamesWishlistTheme {
        WishlistGamesList(
            sections = listOf(
                WishlistSectionUiModel(
                    status = GameStatus.PLAYING,
                    label = UiText.DynamicString("PLAYING"),
                    games = listOf(GameItemUiModel.getDummy())
                ),
                WishlistSectionUiModel(
                    status = null,
                    label = null,
                    games = listOf(GameItemUiModel.getDummy().copy(id = 2, name = "Cyberpunk 2077"))
                )
            ),
            onGameClick = {}
        )
    }
}
