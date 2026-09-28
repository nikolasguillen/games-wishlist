package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface ReleaseNotificationsUiEffect {
    /** A row was just switched on; ask for permission if it is not already granted. */
    data object RequestNotificationPermission : ReleaseNotificationsUiEffect
}
