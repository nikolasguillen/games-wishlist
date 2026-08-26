package com.example.gameswishlist.core.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One popularity signal for a game from IGDB's Popularity API (`/popularity_primitives`), the
 * time-decayed alternative to the raw `hypes` count. Rows are ranked by [value] within a single
 * popularity type; values are not comparable across types, so a feed query pins one type via
 * `where popularity_type = ...` and never requests the field back — the type is already known by
 * whoever issued the query, and requesting it here has nothing to bind it to.
 *
 * @property gameId Id of the game this signal refers to — matches `Game.id` / `IgdbGame.id`.
 * @property value The popularity score; higher is more popular. Only meaningful relative to other
 *   rows fetched by the same query.
 */
@JsonClass(generateAdapter = true)
data class IgdbPopularityPrimitive(
    @Json(name = "game_id") val gameId: Int,
    val value: Double
)
