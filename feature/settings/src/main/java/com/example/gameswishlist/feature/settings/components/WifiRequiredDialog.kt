package com.example.gameswishlist.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.gameswishlist.core.ui.component.CustomAlertDialog
import com.example.gameswishlist.feature.settings.R

@Composable
internal fun WifiRequiredDialog(onDismiss: () -> Unit) {
    CustomAlertDialog(
        title = stringResource(R.string.settings_translation_model_wifi_required_title),
        message = stringResource(R.string.settings_translation_model_wifi_required_message),
        confirmButtonText = stringResource(R.string.settings_translation_model_wifi_required_action),
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}
