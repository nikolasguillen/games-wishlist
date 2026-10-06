package com.nikolasguillen.questlog.core.domain.usecase.list

import com.nikolasguillen.questlog.core.domain.settings.WishlistViewModePreferenceStore
import com.nikolasguillen.questlog.core.model.WishlistViewMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The user's current wishlist layout (list or grid), and every subsequent change. */
class GetWishlistViewModeUseCase @Inject constructor(
    private val wishlistViewModePreferenceStore: WishlistViewModePreferenceStore
) {
    operator fun invoke(): Flow<WishlistViewMode> = wishlistViewModePreferenceStore.observeWishlistViewMode()
}
