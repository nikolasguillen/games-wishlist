package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.gameswishlist.core.designsystem.theme.spacing
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.component.CustomModalBottomSheet
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.WishlistListUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSelectorSheet(
    lists: List<WishlistListUiModel>,
    onDismiss: () -> Unit,
    onListSelected: (Long) -> Unit
) {
    CustomModalBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(min = LocalWindowInfo.current.containerDpSize.height * 0.5f)
                .padding(bottom = MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.select_list_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium)
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            if (lists.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_lists_found_message),
                    modifier = Modifier.padding(MaterialTheme.spacing.medium)
                )
            } else {
                LazyColumn {
                    items(lists) { list ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = list.name.asString(),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
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
                            modifier = Modifier.clickable { onListSelected(list.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
                            thickness = MaterialTheme.spacing.extraSmall / 2,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListSelectorSheetPreview() {
    ListSelectorSheet(
        lists = listOf(
            WishlistListUiModel(1, UiText.DynamicString("Playing"), isSelected = true),
            WishlistListUiModel(2, UiText.DynamicString("Completed"), isSelected = false),
            WishlistListUiModel(3, UiText.DynamicString("Backlog"), isSelected = false)
        ),
        onDismiss = {},
        onListSelected = {}
    )
}
