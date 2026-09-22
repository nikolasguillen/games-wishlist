package com.example.gameswishlist.core.model

/**
 * How precisely a release date is known, mirroring IGDB's deprecated-but-functional
 * `release_dates.category` scalar (0=YYYYMMDD, 1=YYYYMM, 2=YYYY, 3-6=quarters, 7=TBD).
 */
enum class DatePrecision {
    EXACT_DATE,
    YEAR_MONTH,
    YEAR_ONLY,
    QUARTER,
    TBD
}
