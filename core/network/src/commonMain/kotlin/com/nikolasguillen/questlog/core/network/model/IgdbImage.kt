package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.Serializable

/**
 * Represents an image reference of a character or a company: a character's `mug_shot` or a company's `logo`.
 * Same shape as [IgdbCover], which is a game's cover.
 *
 * @property id Internal IGDB unique identifier for the image.
 * @property url The URL of the image. Usually starts with "//", needs "https:" protocol prefix.
 */
@Serializable
data class IgdbImage(
    val id: Int,
    val url: String?
)
