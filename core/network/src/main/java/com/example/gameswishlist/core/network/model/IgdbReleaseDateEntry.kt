package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * A single row from IGDB's `release_dates` endpoint, used by the Radar release-date refresh to backfill
 * dates for games whose detail screen was never opened.
 *
 * @property id Internal IGDB unique identifier.
 * @property game Internal IGDB unique identifier of the game this release date refers to.
 * @property platform The platform this release date refers to. Requested as a nested object (not a bare
 * id) so a platform IGDB knows about but this app hasn't cached yet can still be backfilled.
 * @property date Unix timestamp (seconds) of the release date.
 * @property category IGDB's deprecated-but-functional precision scalar (0=YYYYMMDD, 1=YYYYMM, 2=YYYY,
 * 3-6=quarters, 7=TBD).
 */
@JsonClass(generateAdapter = true)
data class IgdbReleaseDateEntry(
    val id: Int,
    val game: Int,
    val platform: IgdbPlatform?,
    val date: Long?,
    val category: Int?
)
