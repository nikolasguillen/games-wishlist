package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.feature.wishlist.resources.Res
import com.nikolasguillen.questlog.feature.wishlist.resources.delete_action
import com.nikolasguillen.questlog.feature.wishlist.resources.delete_list_dialog_message
import com.nikolasguillen.questlog.feature.wishlist.resources.delete_list_dialog_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun DeleteWishlistDialog(
    listName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomAlertDialog(
        title = stringResource(Res.string.delete_list_dialog_title, listName),
        message = stringResource(Res.string.delete_list_dialog_message),
        confirmButtonText = stringResource(Res.string.delete_action),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiRes.string.cancel),
        onDismiss = onDismiss
    )
}
