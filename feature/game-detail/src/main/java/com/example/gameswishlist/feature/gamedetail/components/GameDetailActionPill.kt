package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.util.animatedMetallicBorder
import com.example.gameswishlist.core.ui.util.brushedMetal

/**
 * A floating action pill for the Game Detail screen.
 * Contains actions to toggle favorite, manage lists, and share.
 */
@Composable
internal fun GameDetailActionPill(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onManageListClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .height(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
                .animatedMetallicBorder(width = 2.dp, shape = CircleShape)
                .width(200.dp)
                .align(Alignment.Center)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .height(72.dp)
                .align(Alignment.Center)
        ) {
            // Favorite Action
            IconButton(
                onClick = onFavoriteClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(com.example.gameswishlist.feature.gamedetail.R.string.favorite_content_description),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            // Main Action: Manage List with "Brushed Metal" effect and custom icon
            IconButton(
                onClick = onManageListClick,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .size(72.dp)
                    .animatedMetallicBorder(width = 2.dp, shape = CircleShape)
                    .padding(MaterialTheme.spacing.smallMedium)
                    .brushedMetal(shape = CircleShape, baseColor = Color.Gray, animateOnce = true)
            ) {
                MachinedPlusIcon(color = Color.Black, modifier = Modifier.size(32.dp))
            }

            // Share Action
            IconButton(
                onClick = onShareClick,
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(com.example.gameswishlist.feature.gamedetail.R.string.share_content_description),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * A custom, bold plus icon designed to look "machined" and premium.
 * Higher stroke weight than standard Material icons.
 */
@Composable
private fun MachinedPlusIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimary
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Horizontal bar
        Spacer(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(4.dp)
                .background(color, shape = CircleShape)
        )
        // Vertical bar
        Spacer(
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .width(4.dp)
                .background(color, shape = CircleShape)
        )
    }
}

@Preview
@Composable
private fun GameDetailActionPillPreview() {
    GamesWishlistTheme {
        GameDetailActionPill(
            isFavorite = true,
            onFavoriteClick = {},
            onManageListClick = {},
            onShareClick = {}
        )
    }
}
