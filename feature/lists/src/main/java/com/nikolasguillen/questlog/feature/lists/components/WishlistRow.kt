package com.nikolasguillen.questlog.feature.lists.components

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.ui.component.CustomSummaryBadge
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.lists.R
import com.nikolasguillen.questlog.feature.lists.model.WishlistListUiModel
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

@Composable
internal fun WishlistRow(
    list: WishlistListUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            WishlistAvatar(
                iconRes = list.iconRes,
                coverImagePath = list.coverImagePath,
                gameCountText = list.gameCountText
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
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
            if (list.isDefault) {
                CustomSummaryBadge(text = stringResource(CoreUiR.string.default_list_label))
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
    coverImagePath: String?,
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
            if (coverImagePath != null) {
                AsyncImage(
                    model = coverImagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    error = painterResource(iconRes),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
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

@QuestLogPreviews
@Composable
private fun WishlistRowPreview() {
    QuestLogTheme {
        WishlistRow(
            list = WishlistListUiModel(
                id = 1,
                name = "RPGs to Try",
                description = "Long ones, for when I have time off",
                iconRes = WishlistIcon.BACKLOG.toDrawableRes(),
                coverImagePath = null,
                gameCountText = UiText.DynamicString("8"),
                isDefault = false
            ),
            onClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun WishlistRowDefaultPreview() {
    QuestLogTheme {
        WishlistRow(
            list = WishlistListUiModel(
                id = 1,
                name = "RPGs to Try",
                description = "Long ones, for when I have time off",
                iconRes = WishlistIcon.BACKLOG.toDrawableRes(),
                coverImagePath = null,
                gameCountText = UiText.DynamicString("8"),
                isDefault = true
            ),
            onClick = {}
        )
    }
}
