package com.nikolasguillen.questlog.core.model

/**
 * A company credited as the developer of at least one game, as a search result.
 *
 * @property id Internal IGDB unique identifier. Navigation and lookups run on this, never on [name].
 * @property name The company's name.
 * @property logoUrl The company's logo, when the catalogue has one.
 * @property developedGameCount How many games IGDB lists under the company as developer, as IGDB reports it. Used
 * only to rank results, not as the number shown on the company's page.
 */
data class Developer(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val developedGameCount: Int
)
