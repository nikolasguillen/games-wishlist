package com.nikolasguillen.questlog.core.database.userdata

import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.relation.GameWithAllDetails

/**
 * Everything the cloud backup carries, as database rows: the user's own data and nothing a refresh could rebuild.
 * It is neither a table nor a query result, so it lives in neither `entity/` nor `relation/`: those must match the
 * exported schema and hold query results.
 *
 * @property lists every list, with its cover path on this device.
 * @property defaultListId the list the default-wishlist pointer row points at.
 * @property memberships which game sits in which list.
 * @property ownedPlatformIds the platforms the user declared they own.
 * @property ownedPlatforms the rows behind [ownedPlatformIds], so the picker can name them before the catalogue syncs.
 * @property savedGames the games the user acted on, with every child collection.
 */
data class UserDataRows(
    val lists: List<ListEntity>,
    val defaultListId: Long,
    val memberships: List<GameListCrossRef>,
    val ownedPlatformIds: List<Int>,
    val ownedPlatforms: List<PlatformEntity>,
    val savedGames: List<GameWithAllDetails>
)
