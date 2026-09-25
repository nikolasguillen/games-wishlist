package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.database.entity.CachedGameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.CachedGameEntity
import com.nikolasguillen.questlog.core.database.entity.CachedGameGenreCrossRef
import com.nikolasguillen.questlog.core.database.entity.CachedGamePlatformCrossRef
import com.nikolasguillen.questlog.core.database.relation.CachedGameWithDetails
import com.nikolasguillen.questlog.core.model.Game
import com.nikolasguillen.questlog.core.model.GameType

/**
 * Entity -> domain for a game cached off a generic Discover lane. Fills every field the lane hydrate
 * query actually requests, so the result is field-identical to [com.nikolasguillen.questlog.core.network.model.IgdbGame.toGame]'s
 * output; every user-owned [Game] field (`notes`, `priority`, `status`, `lastViewedAt`,
 * `detailsFetchedAt`, `isWishlisted`) is left at its default, since [CachedGameEntity] carries none of
 * them.
 */
fun CachedGameWithDetails.toGame(): Game {
    return Game(
        id = game.id,
        name = game.name,
        description = game.description,
        releaseDate = game.released,
        backgroundImage = game.backgroundImage,
        rating = game.rating,
        ratingCount = game.ratingCount,
        hypes = game.hypes,
        metaCritic = game.metacritic,
        platforms = platforms.map { it.toPlatform() },
        genres = genres.map { it.toGenre() },
        developers = companyRefs.filter { it.crossRef.isDeveloper }.map { it.company.toCompany() },
        publishers = companyRefs.filter { it.crossRef.isPublisher }.map { it.company.toCompany() },
        gameType = GameType.fromId(game.gameTypeId),
        url = game.url
    )
}

fun Game.toCachedGameEntity(): CachedGameEntity {
    return CachedGameEntity(
        id = id,
        name = name,
        description = description,
        released = releaseDate,
        backgroundImage = backgroundImage,
        rating = rating,
        ratingCount = ratingCount,
        hypes = hypes,
        metacritic = metaCritic,
        gameTypeId = gameType.id,
        url = url
    )
}

fun Game.toCachedGamePlatformCrossRefs(): List<CachedGamePlatformCrossRef> {
    return platforms.map { CachedGamePlatformCrossRef(gameId = id, platformId = it.id) }
}

fun Game.toCachedGameGenreCrossRefs(): List<CachedGameGenreCrossRef> {
    return genres.map { CachedGameGenreCrossRef(gameId = id, genreId = it.id) }
}

fun Game.toCachedGameCompanyCrossRefs(): List<CachedGameCompanyCrossRef> {
    val companyIds = (developers + publishers).map { it.id }.distinct()
    return companyIds.map { companyId ->
        CachedGameCompanyCrossRef(
            gameId = id,
            companyId = companyId,
            isDeveloper = developers.any { it.id == companyId },
            isPublisher = publishers.any { it.id == companyId }
        )
    }
}
