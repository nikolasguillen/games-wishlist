package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.component.GameListRow
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.util.ColorUtils

@Composable
internal fun WishlistGameRow(
    game: GameItemUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GameListRow(
        coverImage = game.coverImage,
        title = game.name,
        onClick = onClick,
        modifier = modifier
    ) {
        if (game.rating > 0) {
            // Not hoisted into GameListRow: this gap only applies when there is a rating to show, so a
            // shared gap in the shared row would leave a dangling Spacer on unrated games.
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.medium))
            Icon(
                imageVector = ColorUtils.getScoreIcon(game.rating),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = ColorUtils.getScoreColor(game.rating)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
            Text(
                text = game.rating.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ColorUtils.getScoreColor(game.rating)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistGameRowPreview() {
    QuestLogTheme {
        Surface {
            WishlistGameRow(
                game = GameItemUiModel.getDummy(),
                onClick = {}
            )
        }
    }
}
