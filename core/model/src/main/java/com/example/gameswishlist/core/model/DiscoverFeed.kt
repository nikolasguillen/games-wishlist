package com.example.gameswishlist.core.model

/**
 * The Discover feed. [upcoming] is unreleased games ranked by anticipation ("Most anticipated");
 * [popular] is already-released games popular right now ("Popular this month"). Both are generic —
 * they rank the whole catalogue and are the same for every user. Either may be empty when its source
 * returns nothing.
 *
 * [recommended] is the personalised part, and it is `null` far more often than not: a user who has
 * saved nothing has no taste to recommend against, which is the cold-start case the generic shelves
 * exist to cover.
 *
 * [hasStaleRecommendations] is true once the strongest genre in the user's taste profile has moved on
 * from the genre [recommended] was built against — a status, priority or list edit made after this feed
 * was fetched, that would change what the personalised shelf recommends. It is a signal to offer a
 * refresh, not an instruction to fetch one: re-deriving the profile is cheap, but re-fetching the shelf
 * is a network call, and firing one on every library edit would cost far more than the shelf is worth.
 */
data class DiscoverFeed(
    val popular: List<Game>,
    val upcoming: List<Game>,
    val recommended: RecommendedShelf? = null,
    val hasStaleRecommendations: Boolean = false
)
