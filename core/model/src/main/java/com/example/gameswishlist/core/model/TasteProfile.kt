package com.example.gameswishlist.core.model

/**
 * The user's inferred taste, expressed as weighted maps keyed by IGDB id.
 *
 * Weights are normalized to `-1.0..1.0`: a positive weight means the user gravitates towards that
 * genre or developer, a negative one means they have actively dropped games that carried it. The
 * scale is relative to this user's own library, not comparable across users.
 *
 * Keys are ids and never names, so a profile stays valid when a genre is renamed upstream.
 *
 * @property genreWeights Genre id to weight. The baseline signal.
 * @property developerWeights Developer company id to weight. A better predictor than genre for a
 * user with a defined taste, so consumers should weigh a developer hit above a genre hit.
 * @property sampleSize How many saved games the profile was built from. A profile derived from three
 * games is not worth ranking against; consumers can use this to decide whether to trust it.
 */
data class TasteProfile(
    val genreWeights: Map<Int, Double> = emptyMap(),
    val developerWeights: Map<Int, Double> = emptyMap(),
    val sampleSize: Int = 0
) {
    /** True when there is nothing to rank against and callers should fall back to generic content. */
    val isEmpty: Boolean get() = sampleSize == 0

    companion object {
        /** The cold-start profile: the user has saved nothing yet. */
        val EMPTY = TasteProfile()
    }
}
