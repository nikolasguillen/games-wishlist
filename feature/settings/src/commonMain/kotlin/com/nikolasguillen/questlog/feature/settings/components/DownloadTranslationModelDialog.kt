package com.nikolasguillen.questlog.feature.settings.components

import androidx.compose.runtime.Composable
import com.nikolasguillen.questlog.core.ui.component.CustomAlertDialog
import com.nikolasguillen.questlog.core.ui.resources.cancel
import com.nikolasguillen.questlog.feature.settings.resources.Res
import com.nikolasguillen.questlog.feature.settings.resources.settings_translation_model_confirm_action
import com.nikolasguillen.questlog.feature.settings.resources.settings_translation_model_confirm_message
import com.nikolasguillen.questlog.feature.settings.resources.settings_translation_model_confirm_title
import org.jetbrains.compose.resources.stringResource
import com.nikolasguillen.questlog.core.ui.resources.Res as CoreUiRes

@Composable
internal fun DownloadTranslationModelDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    CustomAlertDialog(
        title = stringResource(Res.string.settings_translation_model_confirm_title),
        message = stringResource(Res.string.settings_translation_model_confirm_message),
        confirmButtonText = stringResource(Res.string.settings_translation_model_confirm_action),
        onConfirm = onConfirm,
        dismissButtonText = stringResource(CoreUiRes.string.cancel),
        onDismiss = onDismiss
    )
}
