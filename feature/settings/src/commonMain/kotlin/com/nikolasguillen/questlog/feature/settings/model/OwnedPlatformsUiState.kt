package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerContentState

/**
 * @property selectedCount How many platforms are stored. It cannot be counted off the rendered list,
 * which a search query narrows and which the user may have scrolled away from, and it drives the one
 * line that tells them whether the feed is being filtered at all.
 */
@Immutable
internal data class OwnedPlatformsUiState(
    val contentState: PlatformPickerContentState = PlatformPickerContentState.Loading,
    val selectedCount: Int = 0
)
