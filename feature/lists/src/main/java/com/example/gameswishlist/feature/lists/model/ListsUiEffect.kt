package com.example.gameswishlist.feature.lists.model

internal sealed interface ListsUiEffect {
    data object CoverImageSaveFailed : ListsUiEffect
}
