package com.nikolasguillen.questlog.core.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.nikolasguillen.questlog.core.database.entity.CachedGameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.CachedGameEntity
import com.nikolasguillen.questlog.core.database.entity.CachedGameGenreCrossRef
import com.nikolasguillen.questlog.core.database.entity.CachedGamePlatformCrossRef
import com.nikolasguillen.questlog.core.database.entity.GenreEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity

/**
 * Mirrors [GameWithAllDetails], minus everything the lane hydrate never fetches (engines, artworks,
 * related games, per-platform dates).
 */
data class CachedGameWithDetails(
    @Embedded val game: CachedGameEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CachedGamePlatformCrossRef::class,
            parentColumn = "gameId",
            entityColumn = "platformId"
        )
    )
    val platforms: List<PlatformEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = CachedGameGenreCrossRef::class,
            parentColumn = "gameId",
            entityColumn = "genreId"
        )
    )
    val genres: List<GenreEntity>,
    @Relation(
        entity = CachedGameCompanyCrossRef::class,
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val companyRefs: List<CachedGameCompanyWithDetails>
)
