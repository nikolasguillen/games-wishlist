package com.nikolasguillen.questlog.feature.settings.model

internal sealed interface OwnedPlatformsUiEvent {

    data class OnPlatformToggled(val platformId: Int) : OwnedPlatformsUiEvent

    data object OnClearQuery : OwnedPlatformsUiEvent

    /** The user tapped "Retry" on the empty state, re-syncing the platform catalog. */
    data object OnRetrySync : OwnedPlatformsUiEvent
}
