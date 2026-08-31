package com.example.gameswishlist.core.model

/**
 * A personalised Discover shelf: games chosen because the user's [TasteProfile] leans towards [genre].
 *
 * The genre is carried whole rather than as an id because the shelf has to name it — "every row states
 * its reason" is the rule the Discover feed is built on, and a shelf the user cannot explain reads as a
 * bug. Matching still runs on [Genre.id]; the name is only ever displayed.
 *
 * A shelf only exists when there is a reason for it. Callers get `null` instead of an empty shelf when
 * the profile is too thin to trust or nothing survived the filters.
 *
 * @property genre The signal the shelf was built from, and the thing its title names.
 * @property games The recommendations, already stripped of what the user has saved and of whatever the
 * generic shelves are showing alongside it.
 */
data class RecommendedShelf(
    val genre: Genre,
    val games: List<Game>
)
