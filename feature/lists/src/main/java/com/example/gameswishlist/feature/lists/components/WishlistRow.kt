package com.example.gameswishlist.feature.lists.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.ui.mapper.toDrawableRes
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.lists.R
import com.example.gameswishlist.feature.lists.model.WishlistListUiModel

@Composable
internal fun WishlistRow(list: WishlistListUiModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            modifier = Modifier.padding(MaterialTheme.spacing.mediumLarge)
        ) {
            WishlistAvatar(iconRes = list.iconRes, gameCountText = list.gameCountText)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (list.description.isNotBlank()) {
                    Text(
                        text = list.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WishlistAvatar(
    @DrawableRes iconRes: Int,
    gameCountText: UiText,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(56.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
        val countLabel = gameCountText.asString()
        val countDescription = stringResource(R.string.wishlist_game_count_description, countLabel)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .semantics(mergeDescendants = true) {
                    contentDescription = countDescription
                }
        ) {
            Text(
                text = countLabel,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WishlistRowPreview() {
    GamesWishlistTheme {
        WishlistRow(
            list = WishlistListUiModel(
                id = 1,
                name = "RPGs to Try",
                description = "Long ones, for when I have time off",
                iconRes = WishlistIcon.BACKLOG.toDrawableRes(),
                gameCountText = UiText.DynamicString("8")
            ),
            onClick = {}
        )
    }
}
