package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.feature.settings.R
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

@Composable
internal fun DownloadTranslationModelDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CustomAlertDialog(
        title = stringResource(R.string.settings_translation_model_confirm_title),
        message = stringResource(R.string.settings_translation_model_confirm_message),
        confirmButtonText = stringResource(R.string.settings_translation_model_confirm_action),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiR.string.cancel),
        onDismiss = onDismiss
    )
}
