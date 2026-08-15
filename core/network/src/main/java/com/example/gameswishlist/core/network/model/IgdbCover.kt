package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * Represents a game's cover image reference.
 *
 * @property id Internal IGDB unique identifier for the cover.
 * @property url The URL of the image. Usually starts with "//", needs "https:" protocol prefix.
 */
@JsonClass(generateAdapter = true)
data class IgdbCover(
    val id: Int,
    val url: String?
)
