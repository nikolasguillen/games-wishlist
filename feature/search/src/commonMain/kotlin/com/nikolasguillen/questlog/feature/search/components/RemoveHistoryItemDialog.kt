package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.core.ui.resources.proceed_label
import com.nikolasguillen.questlog.core.ui.util.annotatedStringResource
import com.nikolasguillen.questlog.feature.search.resources.Res
import com.nikolasguillen.questlog.feature.search.resources.remove_history_item
import com.nikolasguillen.questlog.feature.search.resources.remove_history_item_message
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun RemoveHistoryItemDialog(
    query: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomAlertDialog(
        title = stringResource(Res.string.remove_history_item),
        message = annotatedStringResource(Res.string.remove_history_item_message, query),
        confirmButtonText = stringResource(CoreUiRes.string.proceed_label),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiRes.string.cancel),
        onDismiss = onDismiss
    )
}
