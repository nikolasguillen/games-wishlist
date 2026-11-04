package com.nikolasguillen.questlog.feature.search.components

// Item keys stay strings: on Android a lazy item key has to be storable in a Bundle, which a shelf's identity
// (a data class) is not.
//
// Both Discover feeds (compact and wide) emit the same items in the same order with these keys, which is what
// lets a resize across the wide-window threshold keep the first visible shelf: the shared LazyListState
// finds the same item under the same key in the other layout.

/** The hero banner together with its estimate footnote. */
internal const val DISCOVER_HERO_KEY = "hero"

/** The "Most anticipated" shelf. */
internal const val DISCOVER_UPCOMING_KEY = "upcoming"

/** The "Popular this month" shelf. */
internal const val DISCOVER_POPULAR_KEY = "popular"

/** The key of the personalised shelf at [index], strongest first. */
internal fun discoverRecommendedKey(index: Int): String = "recommended_$index"
