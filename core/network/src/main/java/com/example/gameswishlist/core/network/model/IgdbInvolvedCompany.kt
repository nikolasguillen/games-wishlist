package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * Junction model for companies involved in a game's production.
 *
 * @property id Internal IGDB unique identifier for the involved company record.
 * @property company The specific company details.
 * @property developer True if the company acted as a developer for this game.
 * @property publisher True if the company acted as a publisher for this game.
 */
@JsonClass(generateAdapter = true)
data class IgdbInvolvedCompany(
    val id: Int,
    val company: IgdbCompany,
    val developer: Boolean? = false,
    val publisher: Boolean? = false
)
