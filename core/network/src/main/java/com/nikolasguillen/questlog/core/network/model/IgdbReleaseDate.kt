package com.nikolasguillen.questlog.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a release date for a game on a specific platform.
 *
 * @property id Internal IGDB unique identifier.
 * @property date Unix timestamp (seconds) of the release date.
 * @property platform The platform this release date refers to.
 * @property dateFormat IGDB's precision scalar (0=YYYYMMDD, 1=YYYYMM, 2=YYYY, 3-6=quarters, 7=TBD). This
 * replaced the `category` field, which IGDB has stopped returning altogether: asking for the old name
 * yields no value at all, which read back as "exact date" and produced a made-up day for year-only
 * releases. The scalar values themselves are unchanged.
 */
@JsonClass(generateAdapter = true)
data class IgdbReleaseDate(
    val id: Int,
    val date: Long?,
    val platform: IgdbPlatform?,
    @Json(name = "date_format") val dateFormat: Int?
)
