package com.nikolasguillen.questlog.core.ui.mapper

import com.nikolasguillen.questlog.core.common.DateUtils
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.Platform
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.core.ui.resources.Res
import com.nikolasguillen.questlog.core.ui.resources.date_ordinal_format
import com.nikolasguillen.questlog.core.ui.resources.platforms_format
import com.nikolasguillen.questlog.core.ui.resources.rating_title
import com.nikolasguillen.questlog.core.ui.resources.release_date_format
import com.nikolasguillen.questlog.core.ui.resources.release_date_tba
import com.nikolasguillen.questlog.core.ui.resources.score_title
import com.nikolasguillen.questlog.core.ui.resources.status_bought
import com.nikolasguillen.questlog.core.ui.resources.status_completed
import com.nikolasguillen.questlog.core.ui.resources.status_dropped
import com.nikolasguillen.questlog.core.ui.resources.status_playing
import com.nikolasguillen.questlog.core.ui.resources.status_want_to_buy
import com.nikolasguillen.questlog.core.ui.resources.suffix_nd
import com.nikolasguillen.questlog.core.ui.resources.suffix_rd
import com.nikolasguillen.questlog.core.ui.resources.suffix_st
import com.nikolasguillen.questlog.core.ui.resources.suffix_th
import com.nikolasguillen.questlog.core.ui.util.UiConstants
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames
import org.jetbrains.compose.resources.StringResource
import kotlin.math.roundToInt

/**
 * Maps a list of [Game] domain models to a list of [GameItemUiModel]s.
 *
 * [savedIds] flags which games are in the default wishlist; a game whose id isn't in the set maps to
 * `isSaved = false`.
 */
fun List<Game>.toGameItemList(savedIds: Set<Int> = emptySet()): List<GameItemUiModel> {
    return this.map { it.toGameItem(isSaved = it.id in savedIds) }
}

/**
 * Determines the primary rating to display for a game, prioritizing Metacritic.
 */
fun Game.getDisplayRating(): Int = when {
    metaCritic != null && metaCritic!! > 0 -> metaCritic!!
    rating > 0.0 -> rating.roundToInt()
    else -> 0
}

/**
 * Returns the [UiText] representation of the game's rating label (e.g., "Metascore" or "Rating").
 */
fun Game.getRatingUiText(): UiText {
    return when {
        metaCritic != null && metaCritic!! > 0 -> {
            UiText.StringResource(Res.string.score_title)
        }

        rating > 0.0 -> {
            UiText.StringResource(Res.string.rating_title)
        }

        else -> {
            UiText.StringResource(Res.string.rating_title)
        }
    }
}

/**
 * Provides a short, display-friendly version of a platform name.
 */
fun String.getShortPlatformLabel(): String {
    return this.replace(Regex("\\s\\(.*\\)"), "")
}

/**
 * Provides a short, display-friendly version of a platform name.
 */
fun Platform.getShortLabel(): String {
    val cleanedName = name.getShortPlatformLabel()
    val abbr = abbreviation
    return if (cleanedName.length > UiConstants.MAX_PLATFORM_NAME_LENGTH && abbr != null) {
        abbr
    } else {
        cleanedName
    }
}

// Always English, whatever the device language: the card's date format is an English sentence with an
// ordinal suffix ("Mar 1st, 2024"), so localized month names would read wrong inside it.
private val ENGLISH_MONTH_ABBREVIATION = LocalDate.Format { monthName(MonthNames.ENGLISH_ABBREVIATED) }

private fun getOrdinalSuffixRes(day: Int): StringResource {
    if (day in 11..13) return Res.string.suffix_th
    return when (day % 10) {
        1 -> Res.string.suffix_st
        2 -> Res.string.suffix_nd
        3 -> Res.string.suffix_rd
        else -> Res.string.suffix_th
    }
}

/**
 * Maps a [Game] domain model to a [GameItemUiModel].
 *
 * [isSaved] defaults to false so every existing caller (the wishlist screen, Discover shelves, related
 * games) keeps mapping through this function unchanged until it explicitly opts in.
 */
fun Game.toGameItem(isSaved: Boolean = false): GameItemUiModel {
    val year = DateUtils.getYearFromIsoDate(releaseDate)

    val formattedReleaseDate = releaseDate?.let { dateString ->
        try {
            val date = DateUtils.parseIsoDate(dateString) ?: return@let null
            val month = ENGLISH_MONTH_ABBREVIATION.format(date)
            val day = date.day
            val yearVal = date.year
            val suffixRes = getOrdinalSuffixRes(day)

            UiText.StringResource(
                Res.string.date_ordinal_format,
                month,
                day,
                UiText.StringResource(suffixRes),
                yearVal
            )
        } catch (_: Exception) {
            UiText.DynamicString(dateString)
        }
    }

    val cleanedPlatforms = platforms.map { it.getShortLabel() }

    val platformsText = if (cleanedPlatforms.isNotEmpty()) {
        UiText.StringResource(Res.string.platforms_format, cleanedPlatforms.joinToString())
    } else {
        null
    }

    return GameItemUiModel(
        id = id,
        name = name,
        coverImage = backgroundImage,
        rating = getDisplayRating(),
        releaseDateText = formattedReleaseDate?.let {
            UiText.StringResource(Res.string.release_date_format, it)
        } ?: UiText.StringResource(Res.string.release_date_tba),
        releaseYear = year,
        developer = if (developers.isNotEmpty()) developers.joinToString { it.name } else null,
        platforms = platformsText,
        status = status,
        isSaved = isSaved
    )
}

/**
 * Returns the [UiText] label for a [GameStatus] (e.g., "Playing", "Want to buy").
 */
fun GameStatus.toLabelUiText(): UiText {
    val resId = when (this) {
        GameStatus.WANT_TO_BUY -> Res.string.status_want_to_buy
        GameStatus.BOUGHT -> Res.string.status_bought
        GameStatus.PLAYING -> Res.string.status_playing
        GameStatus.COMPLETED -> Res.string.status_completed
        GameStatus.DROPPED -> Res.string.status_dropped
    }
    return UiText.StringResource(resId)
}
