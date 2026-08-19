package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * Represents a game's artwork or screenshot image reference.
 *
 * @property id Internal IGDB unique identifier.
 * @property url The URL of the image. Usually starts with "//", needs "https:" protocol prefix.
 */
@JsonClass(generateAdapter = true)
data class IgdbArtwork(
    val id: Int,
    val url: String?
)
