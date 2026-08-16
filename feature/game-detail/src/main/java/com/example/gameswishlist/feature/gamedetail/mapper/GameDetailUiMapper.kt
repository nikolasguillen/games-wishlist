package com.example.gameswishlist.feature.gamedetail.mapper

import com.example.gameswishlist.core.common.DateUtils
import com.example.gameswishlist.core.domain.model.WishlistAssignment
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.ui.mapper.getDisplayRating
import com.example.gameswishlist.core.ui.mapper.getRatingUiText
import com.example.gameswishlist.core.ui.mapper.toDrawableRes
import com.example.gameswishlist.core.ui.mapper.toGameItem
import com.example.gameswishlist.core.ui.mapper.toUiText
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.PlatformVisuals
import com.example.gameswishlist.feature.gamedetail.R
import com.example.gameswishlist.feature.gamedetail.model.AvailabilityUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailPersonalUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameDetailUiModel
import com.example.gameswishlist.feature.gamedetail.model.GameStatusUiModel
import com.example.gameswishlist.feature.gamedetail.model.PlatformReleaseDateUiModel
import com.example.gameswishlist.feature.gamedetail.model.PlatformTileUiModel
import com.example.gameswishlist.feature.gamedetail.model.PriorityUiModel
import com.example.gameswishlist.feature.gamedetail.model.RatingUiModel
import com.example.gameswishlist.feature.gamedetail.model.RelatedGamesUiModel
import com.example.gameswishlist.feature.gamedetail.model.WishlistListUiModel
import java.util.Locale
import com.example.gameswishlist.core.ui.R as CoreUiR

internal fun Game.toUiModel(): GameDetailUiModel {
    val related = mutableListOf<RelatedGamesUiModel>()

    parentGame?.let {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(CoreUiR.string.related_parent_game),
                games = listOf(it.toGameItem())
            )
        )
    }

    if (dlcs.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(CoreUiR.string.related_dlcs),
                games = dlcs.map { it.toGameItem() }
            )
        )
    }

    if (expansions.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(CoreUiR.string.related_expansions),
                games = expansions.map { it.toGameItem() }
            )
        )
    }

    if (remakes.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(CoreUiR.string.related_remakes),
                games = remakes.map { it.toGameItem() }
            )
        )
    }

    if (remasters.isNotEmpty()) {
        related.add(
            RelatedGamesUiModel(
                title = UiText.StringResource(CoreUiR.string.related_remasters),
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
            hypesLabel = if (hypes > 0) UiText.StringResource(CoreUiR.string.hypes_title) else null,
            ratingCount = if (ratingCount > 0) UiText.DynamicString(formatLargeNumber(ratingCount)) else null,
            ratingCountLabel = if (ratingCount > 0) UiText.StringResource(CoreUiR.string.rating_count_title) else null
        )
    } else null

    val companies = listOfNotNull(
        developers.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() },
        publishers.joinToString(", ") { it.name }.takeIf { it.isNotEmpty() }
    ).joinToString(", ")

    val platformsById = platforms.associateBy { it.id }

    val detailedReleaseDates = releaseDates
        .sortedByDescending { platformsById[it.platformId]?.generation ?: Int.MIN_VALUE }
        .map {
            val platform = platformsById[it.platformId] ?: Platform(
                id = it.platformId,
                name = it.platformName
            )
            val style = PlatformVisuals.styleFor(platform)
            PlatformReleaseDateUiModel(
                platformId = platform.id,
                platformName = UiText.DynamicString(platform.name),
                code = style.code,
                color = style.color,
                date = it.date?.let { date -> UiText.DynamicString(DateUtils.formatUnixTimestamp(date)) }
                    ?: UiText.StringResource(R.string.tba)
            )
        }

    val availability = AvailabilityUiModel(
        mainDate = DateUtils.formatIsoDate(releaseDate)?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(R.string.tba),
        platforms = platforms
            .sortedByDescending { it.generation ?: Int.MIN_VALUE }
            .map {
                val style = PlatformVisuals.styleFor(it)
                PlatformTileUiModel(id = it.id, code = style.code, color = style.color)
            },
        detailedDates = detailedReleaseDates,
        isExpandable = releaseDates.map { it.date }.distinct().size > 1
    )

    return GameDetailUiModel(
        id = id,
        name = UiText.DynamicString(name),
        description = UiText.DynamicString(description),
        images = listOfNotNull(backgroundImage) + artworks,
        gameType = gameType.toUiText(),
        rating = ratingModel,
        availability = availability,
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

internal fun GameStatus.toUiModel(selected: Boolean): GameStatusUiModel {
    val resId = when (this) {
        GameStatus.WANT_TO_BUY -> CoreUiR.string.status_want_to_buy
        GameStatus.BOUGHT -> CoreUiR.string.status_bought
        GameStatus.PLAYING -> CoreUiR.string.status_playing
        GameStatus.COMPLETED -> CoreUiR.string.status_completed
        GameStatus.DROPPED -> CoreUiR.string.status_dropped
    }
    return GameStatusUiModel(
        id = this.id, label = UiText.StringResource(resId), selected = selected
    )
}

internal fun Priority.toUiModel(selected: Boolean): PriorityUiModel {
    val resId = when (this) {
        Priority.LOW -> CoreUiR.string.priority_low
        Priority.MEDIUM -> CoreUiR.string.priority_medium
        Priority.HIGH -> CoreUiR.string.priority_high
    }
    return PriorityUiModel(
        id = this.id, label = UiText.StringResource(resId), selected = selected
    )
}

internal fun WishlistAssignment.toUiModel(): WishlistListUiModel {
    return WishlistListUiModel(
        id = list.id,
        name = UiText.DynamicString(list.name),
        iconRes = list.icon.toDrawableRes(),
        isSelected = isAssigned
    )
}
