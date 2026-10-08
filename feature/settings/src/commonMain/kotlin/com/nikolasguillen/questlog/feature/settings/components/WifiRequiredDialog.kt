package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.got_it
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.settings_translation_model_wifi_required_message
import com.nikolasguillen.questlog.feature.settings.resources.settings_translation_model_wifi_required_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun WifiRequiredDialog(onDismiss: () -> Unit) {
    CustomAlertDialog(
        title = stringResource(Res.string.settings_translation_model_wifi_required_title),
        message = stringResource(Res.string.settings_translation_model_wifi_required_message),
        confirmButtonText = stringResource(CoreUiRes.string.got_it),
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}
