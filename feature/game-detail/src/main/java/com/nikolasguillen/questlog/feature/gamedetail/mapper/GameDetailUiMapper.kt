package com.nikolasguillen.questlog.feature.gamedetail.mapper

import com.nikolasguillen.questlog.core.common.DateUtils
import com.nikolasguillen.questlog.core.domain.model.WishlistAssignment
import com.nikolasguillen.questlog.core.domain.release.canSetStatus
import com.nikolasguillen.questlog.core.domain.release.isPastReleaseDay
import com.nikolasguillen.questlog.core.model.DatePrecision
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.model.Priority
import com.nikolasguillen.questlog.core.ui.mapper.getDisplayRating
import com.nikolasguillen.questlog.core.ui.mapper.getRatingUiText
import com.nikolasguillen.questlog.core.ui.mapper.toDrawableRes
import com.nikolasguillen.questlog.core.ui.mapper.toGameItem
import com.nikolasguillen.questlog.core.ui.mapper.toUiText
import com.nikolasguillen.questlog.core.ui.model.ListSelectorItemUiModel
import com.nikolasguillen.questlog.core.ui.model.PlatformTileUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.util.PlatformVisuals
import com.nikolasguillen.questlog.feature.gamedetail.R
import com.nikolasguillen.questlog.feature.gamedetail.model.AvailabilityUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailPersonalUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameDetailUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.GameStatusUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.PlatformReleaseDateUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.PriorityUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.RatingUiModel
import com.nikolasguillen.questlog.feature.gamedetail.model.RelatedGamesUiModel
import kotlinx.datetime.number
import kotlin.time.Instant
import com.nikolasguillen.questlog.core.ui.R as CoreUiR

internal fun Game.toUiModel(isNotificationEnabled: Boolean, now: Instant): GameDetailUiModel {
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
            score = displayRating.takeIf { it > 0 },
            scoreText = if (displayRating > 0) UiText.DynamicString(displayRating.toString()) else null,
            scoreLabel = if (displayRating > 0) getRatingUiText() else null,
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
                date = formatPlatformReleaseDate(it.date, it.precision)
            )
        }

    val availability = AvailabilityUiModel(
        mainDate = formatMainReleaseDate(releaseDate),
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
        personalDetails = toPersonalUiModel(now),
        relatedGames = related,
        isNotificationEnabled = isNotificationEnabled,
        isNotificationAvailable = isSaved && !isPastReleaseDay(now)
    )
}

private fun Game.toPersonalUiModel(now: Instant): GameDetailPersonalUiModel {
    val lockedStatuses = GameStatus.entries.filterNot { canSetStatus(it, now) }
    return GameDetailPersonalUiModel(
        notes = UiText.DynamicString(notes),
        availableStatuses = GameStatus.entries.map {
            // The current status stays enabled even when locked, so a slipped release can still be cleared.
            it.toUiModel(selected = status == it, enabled = it == status || it !in lockedStatuses)
        },
        lockedStatusesHint = lockedStatuses.takeIf { it.isNotEmpty() }
            ?.let { UiText.StringResource(R.string.status_locked_until_release) },
        availablePriorities = Priority.entries.map { it.toUiModel(selected = this.priority?.id == it.id) }
    )
}

/**
 * Mirrors [com.nikolasguillen.questlog.core.database.dao.GameDao.getSavedGames]'s predicate as closely as
 * a single [Game] allows: in the default wishlist, or carrying a status or a priority. A game that is only
 * in a non-default list has no signal of that on this model -- [isNotificationAvailable] is a display gate
 * only, and [com.nikolasguillen.questlog.core.domain.usecase.notification.SyncReleaseNotificationsUseCase]
 * (working off the full saved-games query) is the actual eligibility enforcement.
 */
private val Game.isSaved: Boolean
    get() = isWishlisted || status != null || priority != null

/**
 * Formats a per-platform release date, respecting [precision] instead of always showing a full day+month+
 * year: a [DatePrecision.YEAR_ONLY] or [DatePrecision.QUARTER] date only ever had that much precision to
 * begin with, so showing more would be a made-up day.
 */
private fun formatPlatformReleaseDate(date: Long?, precision: DatePrecision): UiText {
    if (date == null) return UiText.StringResource(CoreUiR.string.release_date_tba)
    return when (precision) {
        DatePrecision.TBD -> UiText.StringResource(CoreUiR.string.release_date_tba)
        DatePrecision.YEAR_ONLY -> UiText.DynamicString(DateUtils.formatUnixTimestamp(date, "yyyy"))
        DatePrecision.QUARTER -> {
            val localDate = DateUtils.timestampToLocalDate(date)
            val quarter = (localDate.month.number - 1) / 3 + 1
            UiText.StringResource(R.string.quarter_format, quarter, localDate.year)
        }
        DatePrecision.YEAR_MONTH -> UiText.DynamicString(DateUtils.formatUnixTimestamp(date, "MMM yyyy"))
        DatePrecision.EXACT_DATE -> UiText.DynamicString(DateUtils.formatUnixTimestamp(date))
    }
}

/**
 * Formats the game-level release date shown above the platform tiles. Unlike [formatPlatformReleaseDate],
 * this comes from [Game.releaseDate] alone - a plain ISO string with no precision of its own - so
 * [DateUtils.isYearOnlyPlaceholder] is the only signal available that IGDB only ever knew the year.
 */
private fun formatMainReleaseDate(isoDate: String?): UiText {
    if (isoDate == null) return UiText.StringResource(CoreUiR.string.release_date_tba)
    if (DateUtils.isYearOnlyPlaceholder(isoDate)) {
        return DateUtils.getYearFromIsoDate(isoDate)?.let { UiText.DynamicString(it) }
            ?: UiText.StringResource(CoreUiR.string.release_date_tba)
    }
    return DateUtils.formatIsoDate(isoDate)?.let { UiText.DynamicString(it) } ?: UiText.StringResource(CoreUiR.string.release_date_tba)
}

private fun formatLargeNumber(number: Int): String {
    return when {
        number >= 1_000_000 -> "${roundedToTenths(number, 1_000_000)}M"
        number >= 1_000 -> "${roundedToTenths(number, 1_000)}K"
        else -> number.toString()
    }
}

/** `number / divisor` to one decimal place, rounding half up, in plain integer arithmetic. */
private fun roundedToTenths(number: Int, divisor: Int): String {
    val tenths = (number.toLong() * 10 + divisor / 2) / divisor
    return "${tenths / 10}.${tenths % 10}"
}

internal fun GameStatus.toUiModel(selected: Boolean, enabled: Boolean): GameStatusUiModel {
    val resId = when (this) {
        GameStatus.WANT_TO_BUY -> CoreUiR.string.status_want_to_buy
        GameStatus.BOUGHT -> CoreUiR.string.status_bought
        GameStatus.PLAYING -> CoreUiR.string.status_playing
        GameStatus.COMPLETED -> CoreUiR.string.status_completed
        GameStatus.DROPPED -> CoreUiR.string.status_dropped
    }
    return GameStatusUiModel(
        id = this.id, label = UiText.StringResource(resId), selected = selected, enabled = enabled
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

internal fun WishlistAssignment.toUiModel(): ListSelectorItemUiModel {
    return ListSelectorItemUiModel(
        id = list.id,
        name = UiText.DynamicString(list.name),
        iconRes = list.icon.toDrawableRes(),
        isSelected = isAssigned
    )
}
