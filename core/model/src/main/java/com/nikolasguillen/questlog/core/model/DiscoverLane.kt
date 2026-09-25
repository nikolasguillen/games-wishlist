package com.nikolasguillen.questlog.core.model

/**
 * One of the Discover feed's two generic lanes — the ones that rank the whole catalogue and are the
 * same for every user, as opposed to the personalized "recommended" shelves.
 */
enum class DiscoverLane {
    /** Unreleased games ranked by anticipation. */
    MOST_ANTICIPATED,

    /** Already-released games popular right now. */
    POPULAR_THIS_MONTH
}
