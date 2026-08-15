package com.example.gameswishlist.core.model

/**
 * Fixed values of the wishlist feature.
 *
 * @property DEFAULT_WISHLIST_ID Identifier of the list every install starts with, inserted by
 *   `RoomDatabase.Callback.onCreate`. It is also interpolated into `GameDao` queries, so it has to stay a
 *   compile-time constant.
 */
object WishlistConstants {
    const val DEFAULT_WISHLIST_ID = 1L
}
