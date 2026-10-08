package com.nikolasguillen.questlog.feature.lists.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.util.modifiers.dashedBorder
import com.nikolasguillen.questlog.feature.lists.resources.Res
import com.nikolasguillen.questlog.feature.lists.resources.create_list_content_description
import com.nikolasguillen.questlog.feature.lists.resources.create_new_wishlist
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CreateWishlistCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .dashedBorder(
                strokeWidth = 2.dp,
                color = MaterialTheme.appColors.createCardOutlineColor,
                cornerRadius = MaterialTheme.spacing.large
            )
            .clickable(onClick = onClick)
            .padding(MaterialTheme.spacing.large)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.appColors.createCardIconContainerColor)
                .border(
                    width = MaterialTheme.spacing.extraSmall,
                    color = MaterialTheme.appColors.createCardIconBorderColor,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.create_list_content_description),
                tint = MaterialTheme.appColors.createCardIconContentColor
            )
        }
        Text(
            text = stringResource(Res.string.create_new_wishlist),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@QuestLogPreviews
@Composable
private fun CreateWishlistCardPreview() {
    QuestLogTheme {
        CreateWishlistCard(onClick = {})
    }
}
