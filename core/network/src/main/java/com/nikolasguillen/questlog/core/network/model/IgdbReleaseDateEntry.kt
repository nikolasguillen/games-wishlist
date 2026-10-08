package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single row from IGDB's `release_dates` endpoint, used by the Radar release-date refresh to backfill
 * dates for games whose detail screen was never opened.
 *
 * @property id Internal IGDB unique identifier.
 * @property game Internal IGDB unique identifier of the game this release date refers to.
 * @property platform The platform this release date refers to. Requested as a nested object (not a bare
 * id) so a platform IGDB knows about but this app hasn't cached yet can still be backfilled.
 * @property date Unix timestamp (seconds) of the release date.
 * @property dateFormat IGDB's precision scalar (0=YYYYMMDD, 1=YYYYMM, 2=YYYY, 3-6=quarters, 7=TBD). This
 * replaced the `category` field, which IGDB has stopped returning altogether: asking for the old name
 * yields no value at all, which read back as "exact date" and produced a made-up day for year-only
 * releases. The scalar values themselves are unchanged.
 */
@Serializable
data class IgdbReleaseDateEntry(
    val id: Int,
    val game: Int,
    val platform: IgdbPlatform?,
    val date: Long?,
    @SerialName("date_format") val dateFormat: Int?
)
