package com.example.gameswishlist.feature.wishlist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.feature.wishlist.R
import com.example.gameswishlist.core.ui.R as CoreUiR

@Composable
internal fun DeleteWishlistDialog(
    listName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomAlertDialog(
        title = stringResource(R.string.delete_list_dialog_title, listName),
        message = stringResource(R.string.delete_list_dialog_message),
        confirmButtonText = stringResource(R.string.delete_action),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiR.string.cancel),
        onDismiss = onDismiss
    )
}
