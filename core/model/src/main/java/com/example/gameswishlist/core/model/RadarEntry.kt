package com.example.gameswishlist.core.model

/**
 * A saved game paired with one release date the multi-platform resolution decided applies to it. A game
 * with a release date on more than one owned platform produces one [RadarEntry] per owned platform, each
 * carrying that platform's own date; a game matching no owned platform (or none selected) produces a
 * single entry for the earliest date across all its platforms. Carrying the date alongside [game] means a
 * consumer never needs to re-run that resolution just to know which date and precision were used to place
 * the game in its [RadarTimelineSection].
 */
data class RadarEntry(
    val game: Game,
    val releaseDate: ReleaseDate
)
