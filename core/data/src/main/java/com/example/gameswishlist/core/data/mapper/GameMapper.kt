package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.database.entity.CompanyEntity
import com.example.gameswishlist.core.database.entity.GameCompanyCrossRef
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameGenreCrossRef
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.GameWithAllDetails
import com.example.gameswishlist.core.database.entity.GenreEntity
import com.example.gameswishlist.core.database.entity.PlatformEntity
import com.example.gameswishlist.core.model.Company
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.model.Genre
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.network.model.IgdbGame
import com.example.gameswishlist.core.network.model.IgdbGenre
import com.example.gameswishlist.core.network.model.IgdbPlatform
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun IgdbGame.toGame(): Game {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val releasedDate = firstReleaseDate?.let { dateFormat.format(Date(it * 1000)) }

    // IGDB cover URLs start with //, so we add https:
    val imageUrl = cover?.url?.let {
        if (it.startsWith("//")) "https:$it" else it
    }?.replace("t_thumb", "t_720p") // Better quality

    return Game(
        id = id,
        name = name,
        description = summary ?: "",
        releaseDate = releasedDate,
        backgroundImage = imageUrl,
        rating = totalRating ?: 0.0,
        ratingCount = totalRatingCount ?: 0,
        hypes = hypes ?: 0,
        platforms = platforms?.map { it.toPlatform() } ?: emptyList(),
        genres = genres?.map { it.toGenre() } ?: emptyList(),
        publishers = involvedCompanies?.filter { it.publisher }?.map { it.company.toCompany() }
            ?: emptyList(),
        developers = involvedCompanies?.filter { it.developer }?.map { it.company.toCompany() }
            ?: emptyList(),
        gameType = GameType.fromId(gameType)
    )
}

fun IgdbGenre.toGenre(): Genre {
    return Genre(
        id = id,
        name = name
    )
}

fun com.example.gameswishlist.core.network.model.IgdbCompany.toCompany(): Company {
    return Company(
        id = id,
        name = name
    )
}

fun IgdbPlatform.toPlatform(): Platform {
    return Platform(
        id = id,
        name = name,
        abbreviation = abbreviation
    )
}

fun Platform.toEntity(): PlatformEntity {
    return PlatformEntity(
        id = id,
        name = name,
        abbreviation = abbreviation
    )
}

fun PlatformEntity.toPlatform(): Platform {
    return Platform(
        id = id,
        name = name,
        abbreviation = abbreviation
    )
}

fun Genre.toEntity(): GenreEntity {
    return GenreEntity(
        id = id,
        name = name
    )
}

fun GenreEntity.toGenre(): Genre {
    return Genre(
        id = id,
        name = name
    )
}

fun Company.toEntity(): CompanyEntity {
    return CompanyEntity(
        id = id,
        name = name
    )
}

fun CompanyEntity.toCompany(): Company {
    return Company(
        id = id,
        name = name
    )
}

fun GameWithAllDetails.toGame(): Game {
    return Game(
        id = game.id,
        name = game.name,
        description = game.description,
        releaseDate = game.released,
        backgroundImage = game.backgroundImage,
        rating = game.rating,
        metaCritic = game.metacritic,
        platforms = platforms.map { it.toPlatform() },
        genres = genres.map { it.toGenre() },
        developers = companyRefs.filter { it.crossRef.isDeveloper }.map { it.company.toCompany() },
        publishers = companyRefs.filter { it.crossRef.isPublisher }.map { it.company.toCompany() },
        isWishlisted = game.isWishlisted,
        gameType = GameType.fromId(game.gameTypeId),
        notes = game.notes,
        priority = game.priority.toPriority(),
        status = game.status
    )
}

fun Game.toEntity(): GameEntity {
    return GameEntity(
        id = id,
        name = name,
        description = description,
        released = releaseDate,
        backgroundImage = backgroundImage,
        rating = rating,
        metacritic = metaCritic,
        isWishlisted = isWishlisted,
        gameTypeId = gameType.id,
        notes = notes,
        priority = priority.toInt(),
        status = status
    )
}

fun Game.toPlatformEntities(): List<PlatformEntity> {
    return platforms.map { it.toEntity() }
}

fun Game.toGamePlatformCrossRefs(): List<GamePlatformCrossRef> {
    return platforms.map {
        GamePlatformCrossRef(
            gameId = id,
            platformId = it.id
        )
    }
}

fun Game.toGenreEntities(): List<GenreEntity> {
    return genres.map { it.toEntity() }
}

fun Game.toGameGenreCrossRefs(): List<GameGenreCrossRef> {
    return genres.map { GameGenreCrossRef(gameId = id, genreId = it.id) }
}

fun Game.toCompanyEntities(): List<CompanyEntity> {
    val allCompanies = (developers + publishers).distinctBy { it.id }
    return allCompanies.map { it.toEntity() }
}

fun Game.toGameCompanyCrossRefs(): List<GameCompanyCrossRef> {
    val companyIds = (developers + publishers).map { it.id }.distinct()
    return companyIds.map { companyId ->
        GameCompanyCrossRef(
            gameId = id,
            companyId = companyId,
            isDeveloper = developers.any { it.id == companyId },
            isPublisher = publishers.any { it.id == companyId }
        )
    }
}

fun Priority.toInt(): Int {
    return when (this) {
        Priority.LOW -> 0
        Priority.MEDIUM -> 1
        Priority.HIGH -> 2
    }
}

fun Int.toPriority(): Priority {
    return when (this) {
        0 -> Priority.LOW
        1 -> Priority.MEDIUM
        2 -> Priority.HIGH
        else -> Priority.LOW
    }
}