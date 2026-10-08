package com.nikolasguillen.questlog.core.model

/**
 * The Discover feed. [upcoming] is unreleased games ranked by anticipation ("Most anticipated");
 * [popular] is already-released games popular right now ("Popular this month"). Both are generic —
 * they rank the whole catalogue and are the same for every user. Either may be empty when its source
 * returns nothing.
 *
 * [recommended] is the personalised part, one shelf per signal the user leans towards — a recurring
 * developer first when the library earns one, then genres — and it is empty far more often than not: a
 * user who has saved nothing has no taste to recommend against, which is the cold-start case the generic
 * shelves exist to cover. Ordered strongest signal first; a game that qualifies for more than one shelf
 * is kept only in the strongest one it belongs to.
 *
 * [hasStaleRecommendations] is true once the signals the user's taste profile would recommend today have
 * moved on from the ones [recommended] was built against — a status, priority or list edit made after
 * this feed was fetched, that would change what the personalised shelves recommend. It is a signal to
 * offer a refresh, not an instruction to fetch one: re-deriving the profile is cheap, but re-fetching the
 * shelves is a network call, and firing one on every library edit would cost far more than they are worth.
 */
data class DiscoverFeed(
    val popular: List<Game>,
    val upcoming: List<Game>,
    val recommended: List<RecommendedShelf> = emptyList(),
    val hasStaleRecommendations: Boolean = false
)
