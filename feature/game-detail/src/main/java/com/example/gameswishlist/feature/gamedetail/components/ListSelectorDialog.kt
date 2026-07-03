package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.WishlistListUiModel

@Composable
fun ListSelectorDialog(
    lists: List<WishlistListUiModel>,
    onDismiss: () -> Unit,
    onListSelected: (Long) -> Unit
) {
    CustomAlertDialog(
        title = stringResource(R.string.select_list_title),
        dismissButtonText = stringResource(android.R.string.cancel),
        onDismiss = onDismiss,
        content = {
            if (lists.isEmpty()) {
                Text(stringResource(R.string.no_lists_found_message))
            } else {
                LazyColumn {
                    items(lists) { list ->
                        ListItem(
                            headlineContent = { Text(list.name.asString()) },
                            trailingContent = {
                                if (list.isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onListSelected(list.id) }
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ListSelectorDialogPreview() {
    ListSelectorDialog(
        lists = listOf(
            WishlistListUiModel(1, UiText.DynamicString("Playing"), isSelected = true),
            WishlistListUiModel(2, UiText.DynamicString("Completed"), isSelected = false),
            WishlistListUiModel(3, UiText.DynamicString("Backlog"), isSelected = false)
        ),
        onDismiss = {},
        onListSelected = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ListSelectorDialogEmptyPreview() {
    ListSelectorDialog(
        lists = emptyList(),
        onDismiss = {},
        onListSelected = {}
    )
}
