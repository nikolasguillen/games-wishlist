package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.Serializable

/**
 * Represents a game's artwork or screenshot image reference.
 *
 * @property id Internal IGDB unique identifier.
 * @property url The URL of the image. Usually starts with "//", needs "https:" protocol prefix.
 */
@Serializable
data class IgdbArtwork(
    val id: Int,
    val url: String?
)
