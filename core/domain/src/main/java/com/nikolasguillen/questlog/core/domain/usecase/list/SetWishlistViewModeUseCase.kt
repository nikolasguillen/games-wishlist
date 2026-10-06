package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import javax.inject.Inject

/** Persists the wishlist layout the user picked with the view toggle on the wishlist detail screen. */
class SetWishlistViewModeUseCase @Inject constructor(
    private val wishlistViewModePreferenceStore: WishlistViewModePreferenceStore
) {
    suspend operator fun invoke(mode: WishlistViewMode) = wishlistViewModePreferenceStore.setWishlistViewMode(mode)
}
