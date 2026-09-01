package com.example.gameswishlist.feature.search.model

import androidx.compose.runtime.Immutable
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText

/**
 * Lifecycle of the Discover feed, which is the Search screen's *second* content area.
 *
 * It is tracked separately from [SearchContentState] because the two are independent: the feed keeps
 * loading and refreshing while search results hold the screen, and it has to still be there when the
 * user clears the query. Sharing one slot meant keeping a shadow copy of whichever one was not
 * currently displayed.
 *
 * Which of the two is on screen is not stored anywhere — it follows from
 * [SearchContentState.Idle] meaning "no search is active".
 */
@Immutable
internal sealed interface DiscoverContentState {
    data object Loading : DiscoverContentState

    /**
     * [hero] and [upcoming] are disjoint: the top anticipated pick is split out into the hero slot by
     * the mapper, so the composable renders both as given. [recommended] is null whenever the user's
     * library has not earned a personalised shelf.
     */
    data class Content(
        val popular: List<GameItemUiModel>,
        val upcoming: List<GameItemUiModel>,
        val hero: GameItemUiModel? = null,
        val recommended: RecommendedShelfUiModel? = null
    ) : DiscoverContentState

    data class Error(val message: UiText) : DiscoverContentState
}
