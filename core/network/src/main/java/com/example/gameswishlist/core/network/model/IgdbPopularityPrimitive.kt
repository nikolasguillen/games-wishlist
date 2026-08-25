package com.example.gameswishlist.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One popularity signal for a game from IGDB's Popularity API (`/popularity_primitives`), the
 * time-decayed alternative to the raw `hypes` count. Rows are ranked by [value] within a single
 * [popularityType]; values are not comparable across types, so a feed query pins one type.
 *
 * @property gameId Id of the game this signal refers to — matches `Game.id` / `IgdbGame.id`.
 * @property value The popularity score; higher is more popular. Only meaningful relative to other
 *   rows of the same [popularityType].
 * @property popularityType The signal this row measures (e.g. IGDB "Want to Play"). See the type ids
 *   documented on the feed query in `:core:data`.
 */
@JsonClass(generateAdapter = true)
data class IgdbPopularityPrimitive(
    @Json(name = "game_id") val gameId: Int,
    val value: Double,
    @Json(name = "popularity_type") val popularityType: Int
)
