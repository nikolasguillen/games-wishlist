package com.example.gameswishlist.core.model

/**
 * The user's inferred affinity for one genre or developer, as carried by [TasteProfile].
 *
 * @property weight Normalized `-1.0..1.0` score — positive means the user gravitates towards it,
 * negative means they have actively dropped games that carried it. Relative to this user's own
 * library only, never comparable across users.
 * @property count How many saved games back this signal *positively*. A game that contributed a
 * negative weight (a dropped game) is not counted — a rejection is not a recurrence, so [count] means
 * "how many times has the user leaned towards this", not "how many times has it appeared". This is
 * what a normalized [weight] alone cannot tell apart: the strongest genre in a four-game library and
 * the strongest genre in a forty-game one can both score 1.0, and only [count] says which is actually
 * a pattern.
 */
data class TasteSignal(
    val weight: Double,
    val count: Int
)
