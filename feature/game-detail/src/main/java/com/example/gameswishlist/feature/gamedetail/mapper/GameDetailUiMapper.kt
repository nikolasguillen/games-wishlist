package com.example.gameswishlist.feature.gamedetail.mapper

import com.example.gameswishlist.core.common.DateUtils
import com.example.gameswishlist.core.domain.model.WishlistAssignment
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.mapper.getDisplayRating
import com.example.gameswishlist.core.ui.mapper.getRatingUiText
import com.example.gameswishlist.core.ui.mapper.getShortPlatformLabel
import com.example.gameswishlist.core.ui.mapper.toGameItem
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameStatusUiModel
import com.example.gameswishlist.feature.gamedetail.model.PriorityUiModel
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel
import com.example.gameswishlist.feature.gamedetail.model.RelatedGamesUiModel
import com.example.gameswishlist.feature.gamedetail.model.ReleaseInfoUiModel
import com.example.gameswishlist.feature.gamedetail.model.WishlistListUiModel
import java.util.Locale

fun Game.toUiModel(): GameDetailUiModel {
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

    val displayRating = getDisplayRating()
    val ratingModel = if (displayRating > 0 || hypes > 0 || ratingCount > 0) {
        RatingUiModel(
            score = displayRating,
            scoreText = UiText.DynamicString(displayRating.toString()),
            scoreLabel = getRatingUiText(),
            hypes = if (hypes > 0) UiText.DynamicString(formatLargeNumber(hypes)) else null,
            hypesLabel = if (hypes > 0) UiText.StringResource(R.string.hypes_title) else null,
            ratingCount = if (ratingCount > 0) UiText.DynamicString(formatLargeNumber(ratingCount)) else null,
            ratingCountLabel = if (ratingCount > 0) UiText.StringResource(R.string.rating_count_title) else null
        )
    } else null

    val companies = listOfNotNull(
        developers.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() },
        publishers.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() }
    ).joinToString(", ")

    val uiReleaseDates = releaseDates
        .sortedBy { it.date }
        .map {
            val dateString = it.date?.let { date ->
                UiText.DynamicString(DateUtils.formatUnixTimestamp(date))
            } ?: run {
                UiText.StringResource(com.example.gameswishlist.feature.gamedetail.R.string.tba)
            }
            UiText.StringResource(
                R.string.platform_date_format,
                it.platformName.getShortPlatformLabel(),
                dateString
            )
        }

    val releaseInfo = if (releaseDate != null || uiReleaseDates.isNotEmpty()) {
        ReleaseInfoUiModel(
            mainDate = DateUtils.formatIsoDate(releaseDate)?.let { UiText.DynamicString(it) }
                ?: UiText.StringResource(com.example.gameswishlist.feature.gamedetail.R.string.tba),
            detailedMessage = if (uiReleaseDates.isNotEmpty()) {
                UiText.CompoundString(uiReleaseDates, separator = "\n")
            } else null,
            isExpandable = releaseDates.map { it.date }.distinct().size > 1
        )
    } else null

    return GameDetailUiModel(
        id = id,
        name = UiText.DynamicString(name),
        description = UiText.DynamicString(description),
        images = listOfNotNull(backgroundImage) + artworks,
        gameType = gameType.toUiText(),
        rating = ratingModel,
        releaseInfo = releaseInfo,
        platforms = platforms.map { UiText.DynamicString(it.name.getShortPlatformLabel()) },
        genres = genres.map { UiText.DynamicString(it.name) },
        companyInfo = UiText.DynamicString(companies),
        isWishlisted = isWishlisted,
        personalDetails = GameDetailPersonalUiModel(
            notes = UiText.DynamicString(notes),
            availableStatuses = GameStatus.entries.map { it.toUiModel(selected = this.status?.id == it.id) },
            availablePriorities = Priority.entries.map { it.toUiModel(selected = this.priority?.id == it.id) }
        ),
        relatedGames = related
    )
}

private fun formatLargeNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> String.format(Locale.US, "%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format(Locale.US, "%.1fK", number / 1_000.0)
        else -> number.toString()
    }
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

fun WishlistAssignment.toUiModel(): WishlistListUiModel {
    return WishlistListUiModel(
        id = list.id,
        name = UiText.DynamicString(list.name),
        isSelected = isAssigned
    )
}
