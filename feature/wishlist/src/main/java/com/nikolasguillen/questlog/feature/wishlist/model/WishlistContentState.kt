package com.nikolasguillen.questlog.feature.wishlist.model

import androidx.compose.runtime.Immutable

/**
 * Content lifecycle of the wishlist detail screen.
 *
 * [Empty] means the *list* has no games; [FilteredEmpty] means it has games but none match the active status
 * filter. [Success] always holds at least one section, already narrowed by the filter.
 *
 * There is no `Error` case: [com.nikolasguillen.questlog.core.domain.usecase.list.GetWishlistDetailUseCase]
 * observes local storage and has no failure channel. A list that disappears emits `null`, which the
 * ViewModel turns into a [WishlistUiEffect.NavigateBack] rather than a state to render.
 */
@Immutable
internal sealed interface WishlistContentState {
    data object Loading : WishlistContentState
    data object Empty : WishlistContentState

    /** The list has games, but none of them has the status the active filter selects. */
    data object FilteredEmpty : WishlistContentState

    data class Success(val sections: List<WishlistSectionUiModel>) : WishlistContentState
}
