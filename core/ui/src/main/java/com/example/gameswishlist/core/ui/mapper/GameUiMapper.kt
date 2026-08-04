package com.example.gameswishlist.core.ui.mapper

import com.example.gameswishlist.core.common.DateUtils
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameStatus
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import com.example.gameswishlist.core.ui.util.UiConstants
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Maps a list of [Game] domain models to a list of [GameItemUiModel]s.
 */
fun List<Game>.toGameItemList(): List<GameItemUiModel> {
    return this.map { it.toGameItem() }
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
            UiText.StringResource(R.string.score_title)
        }

        rating > 0.0 -> {
            UiText.StringResource(R.string.rating_title)
        }

        else -> {
            UiText.StringResource(R.string.rating_title)
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

private fun getOrdinalSuffixRes(day: Int): Int {
    if (day in 11..13) return R.string.suffix_th
    return when (day % 10) {
        1 -> R.string.suffix_st
        2 -> R.string.suffix_nd
        3 -> R.string.suffix_rd
        else -> R.string.suffix_th
    }
}

/**
 * Maps a [Game] domain model to a [GameItemUiModel].
 */
fun Game.toGameItem(): GameItemUiModel {
    val year = DateUtils.getYearFromIsoDate(releaseDate)

    val formattedReleaseDate = releaseDate?.let { dateString ->
        try {
            val date = DateUtils.parseIsoDate(dateString) ?: return@let null
            val month = date.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
            val day = date.dayOfMonth
            val yearVal = date.year
            val suffixRes = getOrdinalSuffixRes(day)

            UiText.StringResource(
                R.string.date_ordinal_format,
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
        UiText.StringResource(R.string.platforms_format, cleanedPlatforms.joinToString())
    } else {
        null
    }

    return GameItemUiModel(
        id = id,
        name = name,
        coverImage = backgroundImage,
        rating = getDisplayRating(),
        releaseDateText = formattedReleaseDate?.let {
            UiText.StringResource(R.string.release_date_format, it)
        } ?: UiText.StringResource(R.string.unknown_release_date),
        releaseYear = year,
        developer = if (developers.isNotEmpty()) developers.joinToString { it.name } else null,
        platforms = platformsText,
        status = status
    )
}

/**
 * Returns the [UiText] label for a [GameStatus] (e.g., "Playing", "Want to buy").
 */
fun GameStatus.toLabelUiText(): UiText {
    val resId = when (this) {
        GameStatus.WANT_TO_BUY -> R.string.status_want_to_buy
        GameStatus.BOUGHT -> R.string.status_bought
        GameStatus.PLAYING -> R.string.status_playing
        GameStatus.COMPLETED -> R.string.status_completed
        GameStatus.DROPPED -> R.string.status_dropped
    }
    return UiText.StringResource(resId)
}
