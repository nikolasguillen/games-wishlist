package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface ReleaseNotificationsUiEvent {
    data class ToggleOff(val gameId: Int) : ReleaseNotificationsUiEvent
}
