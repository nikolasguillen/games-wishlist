package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import com.nikolasguillen.questlog.core.model.WishlistViewMode

/** Persists the wishlist layout the user picked with the view toggle on the wishlist detail screen. */
class SetWishlistViewModeUseCase(
    private val wishlistViewModePreferenceStore: WishlistViewModePreferenceStore
) {
    suspend operator fun invoke(mode: WishlistViewMode) = wishlistViewModePreferenceStore.setWishlistViewMode(mode)
}
