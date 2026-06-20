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
    val metacritic: Int?,
    val isWishlisted: Boolean,
    val notes: String,
    val priority: Int,
    val status: GameStatus
)

data class GameWithAllDetails(
    @Embedded val game: GameEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GamePlatformCrossRef::class,
            parentColumn = "gameId",
            entityColumn = "platformId"
        )
    )
    val platforms: List<PlatformEntity>,
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
        parentColumn = "id",
        entityColumn = "gameId"
    )
    val companyRefs: List<GameCompanyWithDetails>
)

data class GameCompanyWithDetails(
    @Embedded val crossRef: GameCompanyCrossRef,
    @Relation(
        parentColumn = "companyId",
        entityColumn = "id"
    )
    val company: CompanyEntity
)
