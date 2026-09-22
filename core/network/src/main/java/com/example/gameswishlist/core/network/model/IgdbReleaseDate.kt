package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * Represents a release date for a game on a specific platform.
 *
 * @property id Internal IGDB unique identifier.
 * @property date Unix timestamp (seconds) of the release date.
 * @property platform The platform this release date refers to.
 * @property category IGDB's deprecated-but-functional precision scalar (0=YYYYMMDD, 1=YYYYMM, 2=YYYY,
 * 3-6=quarters, 7=TBD).
 */
@JsonClass(generateAdapter = true)
data class IgdbReleaseDate(
    val id: Int,
    val date: Long?,
    val platform: IgdbPlatform?,
    val category: Int?
)
