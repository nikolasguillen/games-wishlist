package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * The personalised Discover shelf, ready to render.
 *
 * [title] is the reason the shelf exists, not a heading: it names the genre or studio the
 * recommendation came from, so the row explains itself the way the generic shelves cannot.
 */
@Immutable
internal data class RecommendedShelfUiModel(
    val title: UiText,
    val games: List<GameItemUiModel>
)
