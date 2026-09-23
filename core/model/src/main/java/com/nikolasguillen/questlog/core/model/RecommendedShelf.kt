package com.nikolasguillen.questlog.core.model

/**
 * A personalised Discover shelf: games chosen because of [reason].
 *
 * A shelf only exists when there is a reason for it. Callers get `null` instead of an empty shelf when
 * the profile is too thin to trust or nothing survived the filters.
 *
 * @property reason The signal the shelf was built from, and the thing its title names. Matching still
 * runs on the id inside it ([Genre.id] or [Company.id]); the name is only ever displayed.
 * @property games The recommendations, already stripped of what the user has saved and of whatever the
 * generic shelves are showing alongside it.
 */
data class RecommendedShelf(
    val reason: ShelfReason,
    val games: List<Game>
)
