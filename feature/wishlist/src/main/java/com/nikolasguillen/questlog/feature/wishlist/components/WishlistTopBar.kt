package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.feature.wishlist.R
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

@Composable
internal fun WishlistTopBar(
    showEditAction: Boolean,
    showListOptions: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
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
            if (showEditAction) {
                IconButton(
                    onClick = onEditClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_list_action)
                    )
                }
            }
            if (showListOptions) {
                IconButton(
                    onClick = onSetAsDefaultClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = stringResource(R.string.set_as_default_action)
                    )
                }
                IconButton(
                    onClick = onDeleteClick,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_list_action)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier
    )
}

@QuestLogPreviews
@Composable
private fun WishlistTopBarPreview() {
    QuestLogTheme {
        WishlistTopBar(
            showEditAction = true,
            showListOptions = true,
            onBackClick = {},
            onEditClick = {},
            onSetAsDefaultClick = {},
            onDeleteClick = {})
    }
}

@QuestLogPreviews
@Composable
private fun WishlistTopBarDefaultPreview() {
    QuestLogTheme {
        WishlistTopBar(
            showEditAction = true,
            showListOptions = false,
            onBackClick = {},
            onEditClick = {},
            onSetAsDefaultClick = {},
            onDeleteClick = {})
    }
}
