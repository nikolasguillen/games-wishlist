package com.nikolasguillen.questlog.feature.settings.model

import com.nikolasguillen.questlog.core.ui.model.UiText

internal sealed interface AccountBackupUiEffect {
    /** A one-line result: a success such as "Backup complete", or any failure except a closed sign-in. */
    data class ShowMessage(val text: UiText) : AccountBackupUiEffect
}
