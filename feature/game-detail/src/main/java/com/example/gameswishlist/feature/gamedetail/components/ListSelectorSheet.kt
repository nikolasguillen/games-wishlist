package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.gameswishlist.core.designsystem.theme.GamesWishlistTheme
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomModalBottomSheet
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.WishlistListUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSelectorSheet(
    gameName: UiText,
    list: List<WishlistListUiModel>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onToggleList: (Long) -> Unit
) {
    CustomModalBottomSheet(onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(all = MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.add_to_list),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
            )
            Text(
                text = stringResource(R.string.select_list_subtitle, gameName.asString()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            if (list.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_lists_found_message),
                    modifier = Modifier.padding(MaterialTheme.spacing.medium)
                )
            } else {
                LazyColumn {
                    items(list) { item ->
                        WishlistItem(
                            model = item,
                            onCheckedChange = { onToggleList(item.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.large))
                Button(onClick = onConfirm) {
                    Text(text = stringResource(R.string.save_label))
                }
            }
        }
    }
}

@Composable
private fun WishlistItem(
    model: WishlistListUiModel,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        border = BorderStroke(
            1.dp,
            if (model.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .padding(MaterialTheme.spacing.small)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onCheckedChange() }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    MaterialTheme.spacing.medium
                )
        ) {
            Icon(
                painter = painterResource(model.iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (model.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = model.name.asString(),
                modifier = Modifier
                    .padding(MaterialTheme.spacing.medium)
                    .weight(1f)
            )
            Checkbox(
                checked = model.isSelected, onCheckedChange = { onCheckedChange() })
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListSelectorSheetPreview() {
    GamesWishlistTheme {
        ListSelectorSheet(
            gameName = UiText.DynamicString("The Witcher 3"), list = listOf(
                WishlistListUiModel(1, UiText.DynamicString("Playing"), R.drawable.ic_wishlist_playing, isSelected = true),
                WishlistListUiModel(2, UiText.DynamicString("Completed"), R.drawable.ic_wishlist_completed, isSelected = false),
                WishlistListUiModel(3, UiText.DynamicString("Backlog"), R.drawable.ic_wishlist_backlog, isSelected = false)
            ), onDismiss = {}, onConfirm = {}, onToggleList = {})
    }
}
