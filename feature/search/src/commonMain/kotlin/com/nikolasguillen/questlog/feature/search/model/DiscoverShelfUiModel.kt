package com.nikolasguillen.questlog.feature.search.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.GameItemUiModel
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * One Discover shelf, ready to render: a generic lane or a personalised recommendation alike.
 *
 * @property key Which shelf this is. The arrow sends the whole model back, and the ViewModel resolves the
 * shelf's full list of games from this key -- never from [title].
 * @property title The shelf's heading. For a personalised shelf it is the reason the shelf exists, so it
 * names the genre or studio the recommendation came from. It is also what the search bar shows while the
 * shelf's grid is open, so both read the same words.
 * @property games The row: the first `DISCOVER_SHELF_ROW_SIZE` games of the shelf, in the shelf's own order.
 * @property hasMore True exactly when the shelf holds more games than the row shows. It is the only case
 * that gets an arrow, since a grid of the same six games would add nothing.
 */
@Immutable
internal data class DiscoverShelfUiModel(
    val key: DiscoverShelfKey,
    val title: UiText,
    val games: List<GameItemUiModel>,
    val hasMore: Boolean
)
