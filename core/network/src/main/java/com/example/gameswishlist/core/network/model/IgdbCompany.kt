package com.example.gameswishlist.core.network.model

import com.squareup.moshi.JsonClass

/**
 * Represents a company in the gaming industry.
 *
 * @property id Internal IGDB unique identifier for the company.
 * @property name The official name of the company.
 */
@JsonClass(generateAdapter = true)
data class IgdbCompany(
    val id: Int,
    val name: String
)
