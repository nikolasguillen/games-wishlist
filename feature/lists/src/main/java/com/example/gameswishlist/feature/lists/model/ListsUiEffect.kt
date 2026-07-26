package com.example.gameswishlist.feature.lists.model

import com.example.gameswishlist.core.ui.model.UiText

internal sealed interface ListsUiEffect {
    data class ShowSnackbar(val message: UiText) : ListsUiEffect
}
