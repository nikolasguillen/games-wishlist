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

    return GameItem(
        id = id,
        name = name,
        coverImage = backgroundImage,
        ratingText = UiText.StringResource(R.string.rating_format, rating),
        releaseDateText = formattedReleaseDate?.let {
            UiText.StringResource(R.string.release_date_format, it)
        } ?: UiText.StringResource(R.string.unknown_release_date)
    )
}
