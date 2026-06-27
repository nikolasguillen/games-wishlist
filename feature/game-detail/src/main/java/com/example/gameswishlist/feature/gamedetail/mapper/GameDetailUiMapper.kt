package com.example.gameswishlist.feature.gamedetail.mapper

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.mapper.getRatingUiText
import com.example.gameswishlist.core.ui.mapper.toGameItem
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameStatusUiModel
import com.example.gameswishlist.feature.gamedetail.model.PriorityUiModel
import com.example.gameswishlist.feature.gamedetail.model.RelatedGamesUiModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Game.toUiModel(): GameDetailUiModel {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val related = mutableListOf<RelatedGamesUiModel>()

    parentGame?.let {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(R.string.related_parent_game),
                games = listOf(it.toGameItem())
            )
        )
    }

    if (dlcs.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(R.string.related_dlcs),
                games = dlcs.map { it.toGameItem() }
            )
        )
    }

    if (expansions.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(R.string.related_expansions),
                games = expansions.map { it.toGameItem() }
            )
        )
    }

    if (remakes.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(R.string.related_remakes),
                games = remakes.map { it.toGameItem() }
            )
        )
    }

    if (remasters.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(R.string.related_remasters),
                games = remasters.map { it.toGameItem() }
            )
        )
    }

    return GameDetailUiModel(
        id = id,
        name = UiText.DynamicString(name),
        description = UiText.DynamicString(description),
        images = listOfNotNull(backgroundImage) + artworks,
        gameType = gameType.toUiText(),
        ratingText = getRatingUiText(),
        platforms = UiText.DynamicString(platforms.joinToString(", ") { it.name }),
        releaseDates = releaseDates
            .sortedBy { it.date }
            .map {
                val dateString = it.date?.let { date ->
                    UiText.DynamicString(dateFormat.format(Date(date * 1000)))
                } ?: run {
                    UiText.StringResource(com.example.gameswishlist.feature.gamedetail.R.string.tba)
                }
                UiText.DynamicString(it.platformName) to dateString
            },
        genres = genres.map { UiText.DynamicString(it.name) },
        developers = UiText.DynamicString(developers.joinToString(", ") { it.name }),
        publishers = UiText.DynamicString(publishers.joinToString(", ") { it.name }),
        engines = UiText.DynamicString(engines.joinToString(", ")),
        isWishlisted = isWishlisted,
        personalDetails = GameDetailPersonalUiModel(
            notes = UiText.DynamicString(notes),
            availableStatuses = GameStatus.entries.map { it.toUiModel(selected = this.status?.id == it.id) },
            availablePriorities = Priority.entries.map { it.toUiModel(selected = this.priority?.id == it.id) }
        ),
        relatedGames = related
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