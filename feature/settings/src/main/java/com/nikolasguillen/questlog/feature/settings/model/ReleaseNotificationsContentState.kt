package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface ReleaseNotificationsContentState {
    data object Loading : ReleaseNotificationsContentState

    /** No saved games at all -- distinct from having saved games with no reminder enabled yet. */
    data object Empty : ReleaseNotificationsContentState
    data class Success(val games: List<ReleaseNotificationUiModel>) : ReleaseNotificationsContentState
}
