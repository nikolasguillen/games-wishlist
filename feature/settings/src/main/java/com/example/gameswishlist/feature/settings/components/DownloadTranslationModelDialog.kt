package com.example.gameswishlist.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.feature.settings.R
import com.example.gameswishlist.core.ui.R as CoreUiR

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
