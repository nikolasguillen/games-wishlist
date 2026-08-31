package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

/**
 * The personalised Discover shelf, ready to render.
 *
 * [title] is the reason the shelf exists, not a heading: it names the genre the recommendation came
 * from, so the row explains itself the way the generic shelves cannot.
 */
@Immutable
internal data class RecommendedShelfUiModel(
    val title: UiText,
    val games: List<GameItemUiModel>
)
