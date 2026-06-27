package com.example.gameswishlist.core.model

/**
 * Domain model representing a release date for a game on a specific platform.
 *
 * @property date Unix timestamp (seconds) of the release date.
 * @property platformName The name of the platform this release date refers to.
 */
data class ReleaseDate(
    val date: Long?,
    val platformName: String
)
