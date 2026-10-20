package com.nikolasguillen.questlog.core.model

/**
 * One developer with everything its detail page shows.
 *
 * @property id Internal IGDB unique identifier.
 * @property name The company's name.
 * @property logoUrl The company's logo, when the catalogue has one.
 * @property description The catalogue's description, or `null` when it has none (a blank one counts as none).
 * @property foundedYear The year the company was founded (UTC), when the catalogue has a founding date.
 */
data class DeveloperProfile(
    val id: Int,
    val name: String,
    val logoUrl: String?,
    val description: String?,
    val foundedYear: Int?
)
