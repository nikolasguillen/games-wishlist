package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.Serializable

/**
 * Represents a game's cover image reference.
 *
 * @property id Internal IGDB unique identifier for the cover.
 * @property url The URL of the image. Usually starts with "//", needs "https:" protocol prefix.
 */
@Serializable
data class IgdbCover(
    val id: Int,
    val url: String?
)
