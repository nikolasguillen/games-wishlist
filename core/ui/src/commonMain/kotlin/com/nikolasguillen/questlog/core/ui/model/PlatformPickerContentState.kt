package com.nikolasguillen.questlog.core.ui.model

import androidx.compose.runtime.Immutable

/**
 * Lifecycle of the platform picker, shared by Settings and the welcome flow.
 *
 * There is no `Error` branch: the list is rendered off Room, so a failed catalogue sync is not a
 * rendering failure — it just leaves whatever was already cached. Only a sync that fails with nothing
 * cached at all reaches the user, as [Empty].
 */
@Immutable
sealed interface PlatformPickerContentState {

    /** Held until the catalogue, the stored selection and the entry-time order have all arrived. */
    data object Loading : PlatformPickerContentState

    /** Nothing cached to pick from — no saved games and no catalogue sync has landed yet. */
    data object Empty : PlatformPickerContentState

    /** The catalogue has entries, but none of them match what was typed. */
    data object NoSearchResults : PlatformPickerContentState

    data class Success(val platforms: List<PlatformPickerItemUiModel>) : PlatformPickerContentState
}
