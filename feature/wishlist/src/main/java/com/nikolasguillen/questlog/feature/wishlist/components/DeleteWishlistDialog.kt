package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.feature.wishlist.R
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

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
