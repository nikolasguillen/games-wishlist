package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.model.WishlistIcon
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.ic_wishlist_backlog
import com.nikolasguillen.questlog.core.ui.resources.ic_wishlist_collection
import com.nikolasguillen.questlog.core.ui.resources.ic_wishlist_completed
import com.nikolasguillen.questlog.core.ui.resources.ic_wishlist_heart
import com.nikolasguillen.questlog.core.ui.resources.ic_wishlist_multiplayer
import com.nikolasguillen.questlog.core.ui.resources.ic_wishlist_playing
import com.nikolasguillen.questlog.core.ui.resources.placeholder
import org.jetbrains.compose.resources.DrawableResource

fun WishlistIcon?.toDrawableRes(): DrawableResource {
    return when (this) {
        WishlistIcon.PLAYING -> Res.drawable.ic_wishlist_playing
        WishlistIcon.COMPLETED -> Res.drawable.ic_wishlist_completed
        WishlistIcon.BACKLOG -> Res.drawable.ic_wishlist_backlog
        WishlistIcon.HEART -> Res.drawable.ic_wishlist_heart
        WishlistIcon.COLLECTION -> Res.drawable.ic_wishlist_collection
        WishlistIcon.MULTIPLAYER -> Res.drawable.ic_wishlist_multiplayer
        null -> Res.drawable.placeholder
    }
}
