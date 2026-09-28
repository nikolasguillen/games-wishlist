package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface ReleaseNotificationsUiEvent {
    data class SetEnabled(val gameId: Int, val enabled: Boolean) : ReleaseNotificationsUiEvent
}
