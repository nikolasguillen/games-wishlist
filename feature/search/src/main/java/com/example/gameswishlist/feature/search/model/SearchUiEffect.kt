package com.example.gameswishlist.feature.search.model

import com.example.gameswishlist.core.ui.model.UiText

internal sealed interface SearchUiEffect {
    data class ShowSnackbar(val message: UiText) : SearchUiEffect
}
