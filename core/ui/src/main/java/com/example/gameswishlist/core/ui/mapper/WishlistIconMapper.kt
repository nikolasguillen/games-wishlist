package com.example.gameswishlist.core.ui.mapper

import androidx.annotation.DrawableRes
import com.example.gameswishlist.core.model.WishlistIcon
import com.example.gameswishlist.core.ui.R

@DrawableRes
fun WishlistIcon?.toDrawableRes(): Int {
    return when (this) {
        WishlistIcon.PLAYING -> R.drawable.ic_wishlist_playing
        WishlistIcon.COMPLETED -> R.drawable.ic_wishlist_completed
        WishlistIcon.BACKLOG -> R.drawable.ic_wishlist_backlog
        WishlistIcon.HEART -> R.drawable.ic_wishlist_heart
        WishlistIcon.COLLECTION -> R.drawable.ic_wishlist_collection
        WishlistIcon.MULTIPLAYER -> R.drawable.ic_wishlist_multiplayer
        null -> R.drawable.placeholder
    }
}
