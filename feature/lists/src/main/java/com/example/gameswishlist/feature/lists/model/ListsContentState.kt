package com.example.gameswishlist.feature.lists.model

import androidx.compose.runtime.Immutable

/**
 * Content lifecycle of the wishlist hub.
 *
 * There is no `Empty` case: the default wishlist is seeded by `RoomDatabase.Callback.onCreate` and the
 * UI offers no way to delete it, so the hub always has at least one row. [Success] with an empty list
 * still renders correctly — it just shows the creation card on its own.
 */
@Immutable
internal sealed interface ListsContentState {
    data object Loading : ListsContentState
    data class Success(val lists: List<WishlistListUiModel>) : ListsContentState
}
