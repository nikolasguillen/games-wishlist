package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.ui.component.CustomInfoChip
import com.nikolasguillen.questlog.core.ui.component.CustomSummaryBadge
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import java.io.File
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

@Composable
internal fun WishlistDetailHeader(
    title: String,
    description: String?,
    @DrawableRes iconRes: Int,
    coverImageFile: File?,
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
        WishlistCover(iconRes = iconRes, coverImageFile = coverImageFile)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface
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
                    CustomSummaryBadge(text = stringResource(CoreUiR.string.default_list_label))
                }
            }
        }
    }
}

@Composable
private fun WishlistCover(
    @DrawableRes iconRes: Int,
    coverImageFile: File?,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 96.dp, height = 128.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
    ) {
        if (coverImageFile != null) {
            AsyncImage(
                model = coverImageFile,
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
            coverImageFile = null,
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
            coverImageFile = null,
            gameCountText = "1 game",
            isDefaultList = true
        )
    }
}
