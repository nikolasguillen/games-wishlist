package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * One row in the release-notifications management list -- every saved game, not only the currently
 * opted-in ones, so flipping [isEnabled] never removes the row.
 *
 * @property dateLabel The formatted exact release date, or a "no date yet" [UiText] for a game that still
 * resolves to a coarser precision -- what makes FR-013 visible to the user.
 * @property isEnabled Whether the release reminder is currently on for this game.
 */
@Immutable
internal data class ReleaseNotificationUiModel(
    val gameId: Int,
    val coverImage: String?,
    val title: String,
    val dateLabel: UiText,
    val isEnabled: Boolean
)
