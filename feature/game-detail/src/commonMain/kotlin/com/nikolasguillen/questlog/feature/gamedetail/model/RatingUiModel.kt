package com.nikolasguillen.questlog.feature.gamedetail.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.UiText

@Immutable
internal data class RatingUiModel(
    val score: Int?,
    val scoreText: UiText?,
    val scoreLabel: UiText?,
    val hypes: UiText?,
    val hypesLabel: UiText?,
    val ratingCount: UiText?,
    val ratingCountLabel: UiText?
)
