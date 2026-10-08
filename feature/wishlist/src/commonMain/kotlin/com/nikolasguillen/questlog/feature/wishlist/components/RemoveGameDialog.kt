package com.nikolasguillen.questlog.feature.wishlist.components

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.core.ui.resources.remove
import com.nikolasguillen.questlog.feature.wishlist.resources.Res
import com.nikolasguillen.questlog.feature.wishlist.resources.remove_game_dialog_message
import com.nikolasguillen.questlog.feature.wishlist.resources.remove_game_dialog_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun RemoveGameDialog(
    gameName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomAlertDialog(
        title = stringResource(Res.string.remove_game_dialog_title, gameName),
        message = stringResource(Res.string.remove_game_dialog_message),
        confirmButtonText = stringResource(CoreUiRes.string.remove),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiRes.string.cancel),
        onDismiss = onDismiss
    )
}
