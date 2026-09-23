package com.nikolasguillen.questlog.core.model

/**
 * Why a personalised Discover shelf exists — the signal from [TasteProfile] it was built from, carried
 * whole rather than as an id because the shelf has to name it in its title. "Every row states its
 * reason" is the rule the Discover feed is built on; a shelf the user cannot explain reads as a bug.
 *
 * Named `By*` rather than reusing [Genre] / [Company] directly as the variant names, since those types
 * already exist in this package.
 */
sealed interface ShelfReason {
    /** The shelf exists because [developer] recurs across the user's saved games. */
    data class ByDeveloper(val developer: Company) : ShelfReason

    /** The shelf exists because the user's taste profile leans towards [genre]. */
    data class ByGenre(val genre: Genre) : ShelfReason
}
