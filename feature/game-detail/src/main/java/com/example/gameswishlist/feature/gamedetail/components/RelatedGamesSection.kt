package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.component.GameCompactCard
import com.example.gameswishlist.feature.gamedetail.model.RelatedGamesUiModel

@Composable
internal fun RelatedGamesSection(
    relatedGames: List<RelatedGamesUiModel>,
    onGameClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (relatedGames.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        relatedGames.forEach { related ->
            Text(
                text = related.title.asString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.large)
            ) {
                items(related.games) { game ->
                    GameCompactCard(
                        game = game,
                        onClick = { onGameClick(game.id) },
                        modifier = Modifier.width(140.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}
