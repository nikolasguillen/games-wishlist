package com.nikolasguillen.questlog.core.model

import com.nikolasguillen.questlog.core.model.RelationType.DLC
import com.nikolasguillen.questlog.core.model.RelationType.EXPANSION
import com.nikolasguillen.questlog.core.model.RelationType.PARENT
import com.nikolasguillen.questlog.core.model.RelationType.REMAKE
import com.nikolasguillen.questlog.core.model.RelationType.REMASTER


/**
 * How one game relates to another.
 *
 * These are stored as text in `related_games.relationType` and are matched on when splitting a game's
 * relations back apart, so the values are part of the schema: changing a string orphans existing rows.
 *
 * @property DLC Downloadable content for the parent game.
 * @property EXPANSION A larger add-on than a DLC.
 * @property REMAKE A rebuilt version of the game.
 * @property REMASTER The same game with updated assets.
 * @property PARENT The base game this one belongs to.
 */
object RelationType {
    const val DLC = "dlc"
    const val EXPANSION = "expansion"
    const val REMAKE = "remake"
    const val REMASTER = "remaster"
    const val PARENT = "parent"
}
