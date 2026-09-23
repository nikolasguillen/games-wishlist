package com.nikolasguillen.questlog.core.model

/**
 * The user's inferred taste, expressed as [TasteSignal] maps keyed by IGDB id.
 *
 * Keys are ids and never names, so a profile stays valid when a genre or developer is renamed
 * upstream.
 *
 * @property genres Genre id to signal. The baseline signal.
 * @property developers Developer company id to signal. A better predictor than genre for a user with
 * a defined taste — a recurring studio ([TasteSignal.count]) is a stronger recommendation basis than a
 * recurring genre, so consumers should weigh a developer hit above a genre hit.
 * @property sampleSize How many saved games the profile was built from. A profile derived from three
 * games is not worth ranking against; consumers can use this to decide whether to trust it.
 */
data class TasteProfile(
    val genres: Map<Int, TasteSignal> = emptyMap(),
    val developers: Map<Int, TasteSignal> = emptyMap(),
    val sampleSize: Int = 0
) {
    /** True when there is nothing to rank against and callers should fall back to generic content. */
    val isEmpty: Boolean get() = sampleSize == 0

    companion object {
        /** The cold-start profile: the user has saved nothing yet. */
        val EMPTY = TasteProfile()
    }
}
