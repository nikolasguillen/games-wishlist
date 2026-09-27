package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ReleaseNotificationsContentState {
    data object Loading : ReleaseNotificationsContentState
    data object Empty : ReleaseNotificationsContentState
    data class Success(val games: List<ReleaseNotificationUiModel>) : ReleaseNotificationsContentState
}
