package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.feature.wishlist.R
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WishlistTopBar(
    showListOptions: Boolean,
    onBackClick: () -> Unit,
    onSetAsDefaultClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(CoreUiR.string.back_content_description)
                )
            }
        },
        actions = {
            if (showListOptions) {
                ListOptionsMenu(
                    onSetAsDefaultClick = onSetAsDefaultClick,
                    onDeleteClick = onDeleteClick
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    )
}

@Composable
private fun ListOptionsMenu(
    onSetAsDefaultClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.list_options_content_description)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.set_as_default_action)) },
                onClick = {
                    expanded = false
                    onSetAsDefaultClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_list_action)) },
                onClick = {
                    expanded = false
                    onDeleteClick()
                }
            )
        }
    }
}

@QuestLogPreviews
@Composable
private fun WishlistTopBarPreview() {
    QuestLogTheme {
        WishlistTopBar(
            showListOptions = true,
            onBackClick = {},
            onSetAsDefaultClick = {},
            onDeleteClick = {}
        )
    }
}

@QuestLogPreviews
@Composable
private fun WishlistTopBarDefaultPreview() {
    QuestLogTheme {
        WishlistTopBar(
            showListOptions = false,
            onBackClick = {},
            onSetAsDefaultClick = {},
            onDeleteClick = {}
        )
    }
}
