package com.example.gameswishlist.core.model

/**
 * The generic Discover feed shown before any personalisation applies: the two shelves the cold-start
 * design asks for. [upcoming] is unreleased games ranked by anticipation ("Most anticipated"); [popular]
 * is already-released games popular right now ("Popular this month"). Either list may be empty when its
 * source returns nothing.
 */
data class DiscoverFeed(
    val popular: List<Game>,
    val upcoming: List<Game>
)
