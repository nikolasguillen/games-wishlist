package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.core.ui.resources.proceed_label
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.clear_history_message
import com.nikolasguillen.questlog.feature.search.resources.clear_history_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun ClearHistoryDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CustomAlertDialog(
        title = stringResource(Res.string.clear_history_title),
        message = stringResource(Res.string.clear_history_message),
        confirmButtonText = stringResource(CoreUiRes.string.proceed_label),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiRes.string.cancel),
        onDismiss = onDismiss
    )
}
