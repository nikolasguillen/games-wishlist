package com.example.gameswishlist.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Represents a gaming platform.
 *
 * @property id Internal IGDB unique identifier for the platform.
 * @property abbreviation Short form of the platform name (e.g., "PS5", "PC").
 * @property name Full official name of the platform (e.g., "PlayStation 5").
 * @property generation The numerical generation of the platform (e.g., 9 for PS5).
 * @property category Numerical category of the platform (1: Console, 2: Arcade, etc).
 * @property platformFamily Numerical ID of the platform family (1: PlayStation, 2: Xbox, 5: Nintendo).
 */
@JsonClass(generateAdapter = true)
data class IgdbPlatform(
    val id: Int,
    val abbreviation: String?,
    val name: String,
    val generation: Int?,
    val category: Int?,
    @Json(name = "platform_family") val platformFamily: Int?
)
