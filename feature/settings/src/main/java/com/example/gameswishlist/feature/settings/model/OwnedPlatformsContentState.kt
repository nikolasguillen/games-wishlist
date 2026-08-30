package com.example.gameswishlist.feature.settings.model

import androidx.compose.runtime.Immutable

/**
 * Lifecycle of the platform picker.
 *
 * There is no `Error` branch: the list is rendered off Room, so a failed catalogue sync is not a
 * rendering failure — it just leaves whatever was already cached. Only a sync that fails with nothing
 * cached at all reaches the user, as [Empty].
 */
@Immutable
internal sealed interface OwnedPlatformsContentState {

    /** Held until the catalogue, the stored selection and the entry-time order have all arrived. */
    data object Loading : OwnedPlatformsContentState

    /** Nothing cached to pick from — no saved games and no catalogue sync has landed yet. */
    data object Empty : OwnedPlatformsContentState

    /** The catalogue has entries, but none of them match what was typed. */
    data object NoSearchResults : OwnedPlatformsContentState

    data class Success(val platforms: List<PlatformUiModel>) : OwnedPlatformsContentState
}
