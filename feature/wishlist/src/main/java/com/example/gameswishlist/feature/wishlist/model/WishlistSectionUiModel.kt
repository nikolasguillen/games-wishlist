package com.example.gameswishlist.feature.wishlist.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

/**
 * A group of games sharing the same [status], used to render the wishlist detail
 * screen as status-grouped sections (e.g., "Playing", "Want to buy").
 *
 * [status] is null for games with no personal status set yet; [label] is always
 * present so every section renders with a header.
 */
@Immutable
internal data class WishlistSectionUiModel(
    val status: GameStatus?,
    val label: UiText,
    val games: List<GameItemUiModel>
)
