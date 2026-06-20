package com.example.gameswishlist.core.ui.mapper

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.GameItemUiModel
import com.example.gameswishlist.core.ui.model.UiText
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun List<Game>.toGameItemList(): List<GameItemUiModel> {
    return this.map { it.toGameItem() }
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

fun Game.toGameItem(): GameItemUiModel {
    val formattedReleaseDate = releaseDate?.let { dateString ->
        try {
            val date = LocalDate.parse(dateString)
            val month = date.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
            val day = date.dayOfMonth
            val year = date.year
            val suffixRes = getOrdinalSuffixRes(day)

            UiText.StringResource(
                R.string.date_ordinal_format,
                month,
                day,
                UiText.StringResource(suffixRes),
                year
            )
        } catch (_: Exception) {
            UiText.DynamicString(dateString)
        }
    }

    val cleanedRating = if (rating == 0.0) {
        UiText.StringResource(R.string.rating_not_defined)
    } else {
        UiText.StringResource(R.string.rating_format, rating)
    }

    val cleanedPlatforms = platforms.map { 
        (it.abbreviation ?: it.name).replace(Regex("\\s\\(.*\\)"), "") 
    }

    val platformsText = if (cleanedPlatforms.isNotEmpty()) {
        UiText.StringResource(R.string.platforms_format, cleanedPlatforms.joinToString())
    } else {
        null
    }

    return GameItemUiModel(
        id = id,
        name = name,
        coverImage = backgroundImage,
        ratingText = cleanedRating,
        releaseDateText = formattedReleaseDate?.let {
            UiText.StringResource(R.string.release_date_format, it)
        } ?: UiText.StringResource(R.string.unknown_release_date),
        developer = UiText.DynamicString(developers.joinToString()),
        platforms = platformsText
    )
}
