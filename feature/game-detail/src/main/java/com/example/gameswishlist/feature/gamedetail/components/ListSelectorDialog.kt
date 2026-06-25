package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.model.WishlistList
import com.example.gameswishlist.core.ui.R

@Composable
fun ListSelectorDialog(
    lists: List<WishlistList>,
    onDismiss: () -> Unit,
    onListSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_list_title)) },
        text = {
            if (lists.isEmpty()) {
                Text(stringResource(R.string.no_lists_found_message))
            } else {
                LazyColumn {
                    items(lists) { list ->
                        ListItem(
                            headlineContent = { Text(list.name) },
                            modifier = Modifier.clickable { onListSelected(list.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { 
                Text(stringResource(android.R.string.cancel)) 
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ListSelectorDialogPreview() {
    ListSelectorDialog(
        lists = listOf(
            WishlistList(1, "Playing"),
            WishlistList(2, "Completed"),
            WishlistList(3, "Backlog")
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
