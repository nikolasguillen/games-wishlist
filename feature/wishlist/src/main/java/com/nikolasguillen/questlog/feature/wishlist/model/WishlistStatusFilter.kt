package com.nikolasguillen.questlog.feature.wishlist.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.GameStatus

/**
 * Which status section of a wishlist is shown. Held by the ViewModel for as long as the screen lives; it is
 * not persisted.
 *
 * A nullable [GameStatus] alone could not express this: `null` is already the "No status" section, so it
 * could not also mean "no filter".
 */
@Immutable
internal sealed interface WishlistStatusFilter {
    /** Every section is shown. */
    data object All : WishlistStatusFilter

    /** Only the section whose status is [status] is shown; `null` selects the "No status" section. */
    data class Only(val status: GameStatus?) : WishlistStatusFilter
}
