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
 */
data class DiscoverFeed(
    val popular: List<Game>,
    val upcoming: List<Game>,
    val recommended: RecommendedShelf? = null
)
