package com.nikolasguillen.questlog.feature.lists.model

import com.nikolasguillen.questlog.core.model.WishlistIcon

internal sealed interface ListsUiEvent {
    data class OnListCreated(
        val name: String,
        val description: String,
        val icon: WishlistIcon?,
        val coverImageUri: String? = null
    ) : ListsUiEvent
}
