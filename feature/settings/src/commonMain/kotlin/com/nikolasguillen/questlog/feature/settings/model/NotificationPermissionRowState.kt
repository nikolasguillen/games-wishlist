package com.nikolasguillen.questlog.feature.settings.model

/** Mirrors [TranslationModelRowState]'s shape: the row's text is produced by the mapper, not decided by the composable. */
internal sealed interface NotificationPermissionRowState {
    data object Granted : NotificationPermissionRowState
    data object Blocked : NotificationPermissionRowState
}
