package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import kotlinx.coroutines.flow.Flow

/** The user's current wishlist layout (list or grid), and every subsequent change. */
class GetWishlistViewModeUseCase(
    private val wishlistViewModePreferenceStore: WishlistViewModePreferenceStore
) {
    operator fun invoke(): Flow<WishlistViewMode> = wishlistViewModePreferenceStore.observeWishlistViewMode()
}
