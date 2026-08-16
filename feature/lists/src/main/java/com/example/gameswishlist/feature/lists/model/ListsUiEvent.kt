package com.example.gameswishlist.feature.lists.model

import com.example.gameswishlist.core.model.WishlistIcon

internal sealed interface ListsUiEvent {
    data class OnListCreated(
        val name: String,
        val description: String,
        val icon: WishlistIcon?,
        val coverImageUri: String? = null
    ) : ListsUiEvent
}
