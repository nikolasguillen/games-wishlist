package com.example.gameswishlist.core.model

/**
 * A saved game paired with the release date the multi-platform resolution decided applies to it (owned
 * platforms first, falling back to the earliest date across all its platforms). Carrying it alongside
 * [game] means a consumer never needs to re-run that resolution just to know which date and precision were
 * used to place the game in its [RadarTimelineSection].
 */
data class RadarEntry(
    val game: Game,
    val releaseDate: ReleaseDate
)
