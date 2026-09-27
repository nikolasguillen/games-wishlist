package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * One row in the release-notifications management list.
 *
 * @property dateLabel The formatted exact release date, or a "no date yet" [UiText] for an opt-in whose
 * game still resolves to a coarser precision -- what makes FR-013 visible to the user.
 */
@Immutable
internal data class ReleaseNotificationUiModel(
    val gameId: Int,
    val coverImage: String?,
    val title: String,
    val dateLabel: UiText
)
