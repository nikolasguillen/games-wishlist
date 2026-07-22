package com.example.gameswishlist.feature.gamedetail.model

import com.example.gameswishlist.core.ui.model.UiText

internal data class RatingUiModel(
    val score: Int,
    val scoreText: UiText,
    val scoreLabel: UiText,
    val hypes: UiText?,
    val hypesLabel: UiText?,
    val ratingCount: UiText?,
    val ratingCountLabel: UiText?
)
