package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.common.DateUtils
import com.example.gameswishlist.core.database.entity.CompanyEntity
import com.example.gameswishlist.core.database.entity.GameCompanyCrossRef
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameGenreCrossRef
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.GameWithAllDetails
import com.example.gameswishlist.core.database.entity.GenreEntity
import com.example.gameswishlist.core.database.entity.PlatformEntity
import com.example.gameswishlist.core.database.entity.RelatedGameEntity
import com.example.gameswishlist.core.model.Company
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.model.Genre
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.RelationType
import com.example.gameswishlist.core.model.ReleaseDate
import com.example.gameswishlist.core.network.model.IgdbCompany
import com.example.gameswishlist.core.network.model.IgdbGame
import com.example.gameswishlist.core.network.model.IgdbGenre
import com.example.gameswishlist.core.network.model.IgdbPlatform
import com.example.gameswishlist.core.network.model.IgdbReleaseDate

fun IgdbGame.toGame(): Game {
    val releasedDate = firstReleaseDate?.let { DateUtils.formatUnixTimestamp(it, "yyyy-MM-dd") }

    return Game(
        id = id,
        name = name,
        description = summary ?: "",
        releaseDate = releasedDate,
        backgroundImage = cover?.url?.toIgdbImageUrl(),
        rating = totalRating ?: 0.0,
        ratingCount = totalRatingCount ?: 0,
        hypes = hypes ?: 0,
        metaCritic = aggregatedRating?.toInt(),
        platforms = platforms?.map { it.toPlatform() } ?: emptyList(),
        releaseDates = releaseDates?.map { it.toReleaseDate() } ?: emptyList(),
        genres = genres?.map { it.toGenre() } ?: emptyList(),
        publishers = involvedCompanies?.filter { it.publisher == true }
            ?.map { it.company.toCompany() }
            ?: emptyList(),
        developers = involvedCompanies?.filter { it.developer == true }
            ?.map { it.company.toCompany() }
            ?: emptyList(),
        engines = gameEngines?.map { it.name } ?: emptyList(),
        gameType = GameType.fromId(gameType),
        url = url,
        dlcs = dlcList?.map { it.toGame() } ?: emptyList(),
        expansions = expansions?.map { it.toGame() } ?: emptyList(),
        remasters = remasters?.map { it.toGame() } ?: emptyList(),
        remakes = remakes?.map { it.toGame() } ?: emptyList(),
        parentGame = parentGame?.toGame(),
        artworks = if (!screenshots.isNullOrEmpty()) {
            screenshots?.mapNotNull { it.url?.toIgdbImageUrl() }.orEmpty()
        } else {
            artworks?.mapNotNull { it.url?.toIgdbImageUrl() } ?: emptyList()
        }
    )
}

private fun String.toIgdbImageUrl(size: String = "t_720p"): String {
    val formattedUrl = if (this.startsWith("//")) "https:$this" else this
    return formattedUrl.replace("t_thumb", size)
}

fun IgdbGenre.toGenre(): Genre {
    return Genre(
        id = id, name = name
    )
}

fun IgdbCompany.toCompany(): Company {
    return Company(
        id = id, name = name
    )
}

fun IgdbPlatform.toPlatform(): Platform {
    return Platform(
        id = id,
        name = name,
        abbreviation = abbreviation,
        generation = generation,
        category = category,
        platformFamily = platformFamily
    )
}

fun IgdbReleaseDate.toReleaseDate(): ReleaseDate {
    return ReleaseDate(
        date = date,
        platformName = platform?.name ?: "Unknown"
    )
}

fun Platform.toEntity(): PlatformEntity {
    return PlatformEntity(
        id = id,
        name = name,
        abbreviation = abbreviation,
        generation = generation,
        category = category,
        platformFamily = platformFamily
    )
}

fun PlatformEntity.toPlatform(): Platform {
    return Platform(
        id = id,
        name = name,
        abbreviation = abbreviation,
        generation = generation,
        category = category,
        platformFamily = platformFamily
    )
}

fun Genre.toEntity(): GenreEntity {
    return GenreEntity(
        id = id, name = name
    )
}

fun GenreEntity.toGenre(): Genre {
    return Genre(
        id = id, name = name
    )
}

fun Company.toEntity(): CompanyEntity {
    return CompanyEntity(
        id = id, name = name
    )
}

fun CompanyEntity.toCompany(): Company {
    return Company(
        id = id, name = name
    )
}

fun GameWithAllDetails.toGame(): Game {
    val dlcs = relatedGames.filter { it.relationType == RelationType.DLC }.map { it.toGame() }
    val expansions =
        relatedGames.filter { it.relationType == RelationType.EXPANSION }.map { it.toGame() }
    val remakes = relatedGames.filter { it.relationType == RelationType.REMAKE }.map { it.toGame() }
    val remasters =
        relatedGames.filter { it.relationType == RelationType.REMASTER }.map { it.toGame() }
    val parentGame = relatedGames.find { it.relationType == RelationType.PARENT }?.toGame()

    return Game(
        id = game.id,
        name = game.name,
        description = game.description,
        releaseDate = game.released,
        backgroundImage = game.backgroundImage,
        rating = game.rating,
        metaCritic = game.metacritic,
        platforms = platformRefs.map { it.platform.toPlatform() },
        releaseDates = platformRefs.map {
            ReleaseDate(
                platformName = it.platform.name,
                date = it.crossRef.releaseDate
            )
        },
        genres = genres.map { it.toGenre() },
        developers = companyRefs.filter { it.crossRef.isDeveloper }.map { it.company.toCompany() },
        publishers = companyRefs.filter { it.crossRef.isPublisher }.map { it.company.toCompany() },
        engines = game.engines,
        gameType = GameType.fromId(game.gameTypeId),
        notes = game.notes,
        priority = game.priority?.toPriority(),
        status = game.status,
        url = game.url,
        artworks = game.artworks,
        lastViewedAt = game.lastViewedAt,
        dlcs = dlcs,
        expansions = expansions,
        remakes = remakes,
        remasters = remasters,
        parentGame = parentGame
    )
}

fun RelatedGameEntity.toGame(): Game {
    return Game(
        id = relatedGameId, name = name, backgroundImage = coverUrl
    )
}

fun Game.toRelatedGameEntities(): List<RelatedGameEntity> {
    val related = mutableListOf<RelatedGameEntity>()

    dlcs.forEach {
        related.add(
            RelatedGameEntity(
                parentId = id,
                relatedGameId = it.id,
                name = it.name,
                coverUrl = it.backgroundImage,
                relationType = RelationType.DLC
            )
        )
    }
    expansions.forEach {
        related.add(
            RelatedGameEntity(
                parentId = id,
                relatedGameId = it.id,
                name = it.name,
                coverUrl = it.backgroundImage,
                relationType = RelationType.EXPANSION
            )
        )
    }
    remakes.forEach {
        related.add(
            RelatedGameEntity(
                parentId = id,
                relatedGameId = it.id,
                name = it.name,
                coverUrl = it.backgroundImage,
                relationType = RelationType.REMAKE
            )
        )
    }
    remasters.forEach {
        related.add(
            RelatedGameEntity(
                parentId = id,
                relatedGameId = it.id,
                name = it.name,
                coverUrl = it.backgroundImage,
                relationType = RelationType.REMASTER
            )
        )
    }
    parentGame?.let {
        related.add(
            RelatedGameEntity(
                parentId = id,
                relatedGameId = it.id,
                name = it.name,
                coverUrl = it.backgroundImage,
                relationType = RelationType.PARENT
            )
        )
    }

    return related
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
        gameTypeId = gameType.id,
        notes = notes,
        priority = priority?.toInt(),
        status = status,
        url = url,
        artworks = artworks,
        engines = engines,
        lastViewedAt = lastViewedAt
    )
}

fun Game.toPlatformEntities(): List<PlatformEntity> {
    return platforms.map { it.toEntity() }
}

fun Game.toGamePlatformCrossRefs(): List<GamePlatformCrossRef> {
    return platforms.map { platform ->
        val releaseDate = releaseDates.find { it.platformName == platform.name }?.date
        GamePlatformCrossRef(
            gameId = id,
            platformId = platform.id,
            releaseDate = releaseDate
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
            isPublisher = publishers.any { it.id == companyId })
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