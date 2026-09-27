package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class ReleaseNotificationsUiState(
    val contentState: ReleaseNotificationsContentState = ReleaseNotificationsContentState.Loading
)
