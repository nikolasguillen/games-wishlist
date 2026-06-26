package com.example.gameswishlist.feature.gamedetail.mapper

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.mapper.getRatingUiText
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameStatusUiModel
import com.example.gameswishlist.feature.gamedetail.model.PriorityUiModel

fun Game.toUiModel(): GameDetailUiModel {
    return GameDetailUiModel(
        id = id,
        name = name,
        description = description,
        backgroundImage = backgroundImage,
        gameType = gameType.toUiText(),
        ratingText = getRatingUiText(),
        platforms = platforms.map { it.name },
        genres = genres.map { it.name },
        isWishlisted = isWishlisted,
        personalDetails = GameDetailPersonalUiModel(
            notes = notes,
            availableStatuses = GameStatus.entries.map { it.toUiModel(selected = this.status?.id == it.id) },
            availablePriorities = Priority.entries.map { it.toUiModel(selected = this.priority?.id == it.id) })
    )
}

fun GameStatus.toUiModel(selected: Boolean): GameStatusUiModel {
    val resId = when (this) {
        GameStatus.WANT_TO_BUY -> R.string.status_want_to_buy
        GameStatus.BOUGHT -> R.string.status_bought
        GameStatus.PLAYING -> R.string.status_playing
        GameStatus.COMPLETED -> R.string.status_completed
        GameStatus.DROPPED -> R.string.status_dropped
    }
    return GameStatusUiModel(
        id = this.id, label = UiText.StringResource(resId), selected = selected
    )
}

fun Priority.toUiModel(selected: Boolean): PriorityUiModel {
    val resId = when (this) {
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
    }
    return PriorityUiModel(
        id = this.id, label = UiText.StringResource(resId), selected = selected
    )
}