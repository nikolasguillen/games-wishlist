package com.nikolasguillen.questlog.feature.wishlist.components

import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.ui.component.CustomInfoChip
import com.nikolasguillen.questlog.core.ui.component.CustomSummaryBadge
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.resources.default_list_label
import com.nikolasguillen.questlog.core.ui.util.modifiers.rememberCoverBrush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun WishlistDetailHeader(
    title: String,
    description: String?,
    iconRes: DrawableResource,
    coverImagePath: String?,
    gameCountText: String,
    isDefaultList: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(MaterialTheme.spacing.large)
    ) {
        WishlistCover(iconRes = iconRes, coverImagePath = coverImagePath)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
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
                modifier = Modifier.padding(top = MaterialTheme.spacing.mediumLarge)
            ) {
                CustomInfoChip(
                    text = gameCountText,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    isLarge = false
                )
                if (isDefaultList) {
                    CustomSummaryBadge(text = stringResource(CoreUiRes.string.default_list_label))
                }
            }
        }
    }
}

@Composable
private fun WishlistCover(
    iconRes: DrawableResource,
    coverImagePath: String?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 96.dp, height = 128.dp)
            .clip(MaterialTheme.shapes.large)
            .background(rememberCoverBrush())
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        if (coverImagePath != null) {
            AsyncImage(
                model = coverImagePath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                error = painterResource(iconRes),
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun WishlistDetailHeaderPreview() {
    QuestLogTheme {
        WishlistDetailHeader(
            title = "Couch Co-op",
            description = "Games worth playing together, controllers in hand.",
            iconRes = WishlistIcon.MULTIPLAYER.toDrawableRes(),
            coverImagePath = null,
            gameCountText = "12 games",
            isDefaultList = false
        )
    }
}

@QuestLogPreviews
@Composable
private fun WishlistDetailHeaderDefaultNoDescriptionPreview() {
    QuestLogTheme {
        WishlistDetailHeader(
            title = "Wishlist",
            description = null,
            iconRes = WishlistIcon.HEART.toDrawableRes(),
            coverImagePath = null,
            gameCountText = "1 game",
            isDefaultList = true
        )
    }
}
