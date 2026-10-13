package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.DiscoverLane

/**
 * Which Discover shelf a row or a grid is.
 *
 * A shelf is named by the id of the signal it was built from -- never by its displayed title, and never by
 * its position: a feed refresh can reorder the personalised shelves between the moment a row is rendered and
 * the moment its arrow is tapped, and a position would then open a different shelf under the title the user
 * tapped. Within one feed a key is unique, since the feed builds at most one shelf per signal and developer
 * and genre shelves are separate cases.
 */
@Immutable
internal sealed interface DiscoverShelfKey {
    /** One of the two generic lanes, the same for every user. */
    data class Lane(val lane: DiscoverLane) : DiscoverShelfKey

    /** The personalised shelf built from the genre with this IGDB id. */
    data class ByGenre(val genreId: Int) : DiscoverShelfKey

    /** The personalised shelf built from the developer with this IGDB company id. */
    data class ByDeveloper(val developerId: Int) : DiscoverShelfKey
}
