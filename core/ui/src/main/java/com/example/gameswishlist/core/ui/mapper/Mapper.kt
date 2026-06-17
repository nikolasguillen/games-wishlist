package com.example.gameswishlist.core.ui.mapper

import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.ui.R
import com.example.gameswishlist.core.ui.model.GameItem
import com.example.gameswishlist.core.ui.model.UiText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun List<Game>.toGameItemList(): List<GameItem> {
    return this.map { it.toGameItem() }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun Game.toGameItem(): GameItem {
    val formattedReleaseDate = releaseDate?.let { dateString ->
        try {
            val date = LocalDate.parse(dateString) // RAWG format is YYYY-MM-DD
            date.format(dateFormatter)
        } catch (_: Exception) {
            dateString
        }
    }

    val cleanedRating = if (rating == 0.0) {
        UiText.StringResource(R.string.rating_not_defined)
    } else {
        UiText.StringResource(R.string.rating_format, rating)
    }

    val developersText = if (developers.isNotEmpty()) {
        UiText.StringResource(R.string.developer_format, developers.joinToString())
    } else {
        null
    }

    val cleanedPlatforms = platforms.map { it.replace(Regex("\\s\\(.*\\)"), "") }

    val platformsText = if (cleanedPlatforms.isNotEmpty()) {
        UiText.StringResource(R.string.platforms_format, cleanedPlatforms.joinToString())
    } else {
        null
    }

    return GameItem(
        id = id,
        name = name,
        coverImage = backgroundImage,
        ratingText = cleanedRating,
        releaseDateText = formattedReleaseDate?.let {
            UiText.StringResource(R.string.release_date_format, it)
        } ?: UiText.StringResource(R.string.unknown_release_date),
        developer = developersText,
        platforms = platformsText
    )
}
