package com.nikolasguillen.questlog.core.database.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.nikolasguillen.questlog.core.database.entity.EngineEntity
import com.nikolasguillen.questlog.core.database.entity.GameArtworkEntity
import com.nikolasguillen.questlog.core.database.entity.GameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameEngineCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameEntity
import com.nikolasguillen.questlog.core.database.entity.GameGenreCrossRef
import com.nikolasguillen.questlog.core.database.entity.GamePlatformCrossRef
import com.nikolasguillen.questlog.core.database.entity.GenreEntity
import com.nikolasguillen.questlog.core.database.entity.RelatedGameEntity

data class GameWithAllDetails(
    @Embedded val game: GameEntity,
    @Relation(
        entity = GamePlatformCrossRef::class,
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val platformRefs: List<GamePlatformWithDetails>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GameGenreCrossRef::class,
            parentColumn = "gameId",
            entityColumn = "genreId"
        )
    )
    val genres: List<GenreEntity>,
    @Relation(
        entity = GameCompanyCrossRef::class,
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val companyRefs: List<GameCompanyWithDetails>,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentId"
    )
    val relatedGames: List<RelatedGameEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GameEngineCrossRef::class,
            parentColumn = "gameId",
            entityColumn = "engineId"
        )
    )
    val engines: List<EngineEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val artworks: List<GameArtworkEntity>
)
