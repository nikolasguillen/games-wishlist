package com.example.gameswishlist.feature.wishlist.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.feature.wishlist.R
import com.example.gameswishlist.core.ui.R as CoreUiR

@Composable
internal fun RemoveGameDialog(
    gameName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomAlertDialog(
        title = stringResource(R.string.remove_game_dialog_title, gameName),
        message = stringResource(R.string.remove_game_dialog_message),
        confirmButtonText = stringResource(CoreUiR.string.remove),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiR.string.cancel),
        onDismiss = onDismiss
    )
}
