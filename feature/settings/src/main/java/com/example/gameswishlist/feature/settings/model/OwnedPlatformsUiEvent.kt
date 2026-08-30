package com.example.gameswishlist.feature.settings.model

internal sealed interface OwnedPlatformsUiEvent {

    data class OnPlatformToggled(val platformId: Int) : OwnedPlatformsUiEvent

    data object OnClearQuery : OwnedPlatformsUiEvent
}
