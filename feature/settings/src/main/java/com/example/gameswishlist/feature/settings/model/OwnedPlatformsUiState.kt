package com.example.gameswishlist.feature.settings.model

import androidx.compose.runtime.Immutable

/**
 * @property selectedCount How many platforms are stored. It cannot be counted off the rendered list,
 * which a search query narrows and which the user may have scrolled away from, and it drives the one
 * line that tells them whether the feed is being filtered at all.
 */
@Immutable
internal data class OwnedPlatformsUiState(
    val contentState: OwnedPlatformsContentState = OwnedPlatformsContentState.Loading,
    val selectedCount: Int = 0
)
