package com.example.gameswishlist.feature.wishlist.model

import androidx.compose.runtime.Immutable

/**
 * Content lifecycle of the wishlist detail screen.
 *
 * There is no `Error` case: [com.example.gameswishlist.core.domain.usecase.list.GetWishlistDetailUseCase]
 * observes local storage and has no failure channel. A list that disappears emits `null`, which the
 * ViewModel turns into a [WishlistUiEffect.NavigateBack] rather than a state to render.
 */
@Immutable
internal sealed interface WishlistContentState {
    data object Loading : WishlistContentState
    data object Empty : WishlistContentState
    data class Success(val sections: List<WishlistSectionUiModel>) : WishlistContentState
}
