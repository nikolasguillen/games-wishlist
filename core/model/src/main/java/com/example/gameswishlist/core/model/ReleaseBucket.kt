package com.example.gameswishlist.core.model

/** A calendar-relative time window a saved game's resolved release date falls into, for the Radar timeline. */
enum class ReleaseBucket {
    RECENTLY_RELEASED,
    THIS_WEEK,
    THIS_MONTH,
    NEXT_3_MONTHS,
    LATER,
    TBA
}
