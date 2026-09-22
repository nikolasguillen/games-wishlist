package com.example.gameswishlist.core.model

/**
 * One non-empty section of the Radar timeline: every saved game whose resolved release date falls into
 * [bucket], sorted ascending by date (alphabetically within [ReleaseBucket.TBA], which has no date).
 */
data class RadarTimelineSection(
    val bucket: ReleaseBucket,
    val entries: List<RadarEntry>
)
