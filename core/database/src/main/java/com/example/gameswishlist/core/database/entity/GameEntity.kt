package com.example.gameswishlist.core.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.gameswishlist.core.model.GameStatus

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val released: String?,
    val backgroundImage: String?,
    val rating: Double,
    val ratingCount: Int = 0,
    val hypes: Int = 0,
    val metacritic: Int?,
    val gameTypeId: Int,
    val notes: String,
    val priority: Int?,
    val status: GameStatus?,
    val url: String?,
    val lastViewedAt: Long? = null
)

@Entity(
    tableName = "related_games",
    primaryKeys = ["parentId", "relatedGameId", "relationType"]
)
data class RelatedGameEntity(
    val parentId: Int,
    val relatedGameId: Int,
    val name: String,
    val coverUrl: String?,
    val relationType: String
)

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

data class GamePlatformWithDetails(
    @Embedded val crossRef: GamePlatformCrossRef,
    @Relation(
        parentColumn = "platformId",
        entityColumn = "id"
    )
    val platform: PlatformEntity
)

data class GameCompanyWithDetails(
    @Embedded val crossRef: GameCompanyCrossRef,
    @Relation(
        parentColumn = "companyId",
        entityColumn = "id"
    )
    val company: CompanyEntity
)
