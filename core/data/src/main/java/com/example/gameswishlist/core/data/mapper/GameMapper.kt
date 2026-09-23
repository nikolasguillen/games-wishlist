package com.example.gameswishlist.core.data.mapper

import com.example.gameswishlist.core.common.DateUtils
import com.example.gameswishlist.core.database.entity.CompanyEntity
import com.example.gameswishlist.core.database.entity.EngineEntity
import com.example.gameswishlist.core.database.entity.GameArtworkEntity
import com.example.gameswishlist.core.database.entity.GameCompanyCrossRef
import com.example.gameswishlist.core.database.entity.GameEngineCrossRef
import com.example.gameswishlist.core.database.entity.GameEntity
import com.example.gameswishlist.core.database.entity.GameGenreCrossRef
import com.example.gameswishlist.core.database.entity.GamePlatformCrossRef
import com.example.gameswishlist.core.database.entity.GenreEntity
import com.example.gameswishlist.core.database.entity.PlatformEntity
import com.example.gameswishlist.core.database.entity.RelatedGameEntity
import com.example.gameswishlist.core.database.relation.GameWithAllDetails
import com.example.gameswishlist.core.model.Company
import com.example.gameswishlist.core.model.DatePrecision
import com.example.gameswishlist.core.model.DatePrecision.EXACT_DATE
import com.example.gameswishlist.core.model.DatePrecision.QUARTER
import com.example.gameswishlist.core.model.DatePrecision.TBD
import com.example.gameswishlist.core.model.DatePrecision.YEAR_MONTH
import com.example.gameswishlist.core.model.DatePrecision.YEAR_ONLY
import com.example.gameswishlist.core.model.Engine
import com.example.gameswishlist.core.model.Game
import com.example.gameswishlist.core.model.GameType
import com.example.gameswishlist.core.model.Genre
import com.example.gameswishlist.core.model.Platform
import com.example.gameswishlist.core.model.Priority
import com.example.gameswishlist.core.model.RelationType
import com.example.gameswishlist.core.model.ReleaseDate
import com.example.gameswishlist.core.network.model.IgdbCompany
import com.example.gameswishlist.core.network.model.IgdbGame
import com.example.gameswishlist.core.network.model.IgdbGameEngine
import com.example.gameswishlist.core.network.model.IgdbGenre
import com.example.gameswishlist.core.network.model.IgdbPlatform
import com.example.gameswishlist.core.network.model.IgdbReleaseDate
import com.example.gameswishlist.core.network.model.IgdbReleaseDateEntry

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
        engines = gameEngines?.map { it.toEngine() } ?: emptyList(),
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

fun IgdbGameEngine.toEngine(): Engine {
    return Engine(
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
        platformId = platform?.id ?: 0,
        platformName = platform?.name ?: "Unknown",
        precision = fromIgdbDateFormat(dateFormat)
    )
}

/**
 * Maps a Radar release-date refresh response into per-(game, platform) cross-refs, paired with the
 * [PlatformEntity] to backfill if that platform isn't cached yet (null when IGDB returned no platform
 * object for a row, which is dropped since a cross-ref needs a platform id).
 *
 * No `region` filter is applied upstream, so duplicate regional rows can land for the same
 * (game, platform); the earliest [IgdbReleaseDateEntry.date] wins, sidestepping the deprecated `region`
 * enum entirely.
 */
fun List<IgdbReleaseDateEntry>.toGamePlatformCrossRefs(): List<Pair<GamePlatformCrossRef, PlatformEntity?>> {
    return this
        .filter { it.platform != null }
        .groupBy { it.game to it.platform!!.id }
        .map { (_, entries) ->
            val earliest = entries.minBy { it.date ?: Long.MAX_VALUE }
            val crossRef = GamePlatformCrossRef(
                gameId = earliest.game,
                platformId = earliest.platform!!.id,
                releaseDate = earliest.date,
                releaseDatePrecision = earliest.dateFormat
            )
            crossRef to earliest.platform?.toPlatform()?.toEntity()
        }
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

fun Engine.toEntity(): EngineEntity {
    return EngineEntity(
        id = id, name = name
    )
}

fun EngineEntity.toEngine(): Engine {
    return Engine(
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
        ratingCount = game.ratingCount,
        hypes = game.hypes,
        metaCritic = game.metacritic,
        platforms = platformRefs.map { it.platform.toPlatform() },
        releaseDates = platformRefs.map {
            ReleaseDate(
                platformId = it.platform.id,
                platformName = it.platform.name,
                date = it.crossRef.releaseDate,
                precision = storedPrecision(it.crossRef.releaseDatePrecision, it.crossRef.releaseDate)
            )
        },
        genres = genres.map { it.toGenre() },
        developers = companyRefs.filter { it.crossRef.isDeveloper }.map { it.company.toCompany() },
        publishers = companyRefs.filter { it.crossRef.isPublisher }.map { it.company.toCompany() },
        engines = engines.map { it.toEngine() },
        gameType = GameType.fromId(game.gameTypeId),
        notes = game.notes,
        priority = game.priority?.toPriority(),
        status = game.status,
        url = game.url,
        // @Relation cannot sort, so the gallery order is restored here from the stored position.
        artworks = artworks.sortedBy { it.position }.map { it.url },
        lastViewedAt = game.lastViewedAt,
        detailsFetchedAt = game.detailsFetchedAt,
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
        ratingCount = ratingCount,
        hypes = hypes,
        metacritic = metaCritic,
        gameTypeId = gameType.id,
        notes = notes,
        priority = priority?.toInt(),
        status = status,
        url = url,
        lastViewedAt = lastViewedAt,
        detailsFetchedAt = detailsFetchedAt
    )
}

fun Game.toEngineEntities(): List<EngineEntity> {
    return engines.map { it.toEntity() }
}

fun Game.toGameEngineCrossRefs(): List<GameEngineCrossRef> {
    return engines.map { GameEngineCrossRef(gameId = id, engineId = it.id) }
}

fun Game.toArtworkEntities(): List<GameArtworkEntity> {
    return artworks.mapIndexed { position, url ->
        GameArtworkEntity(gameId = id, position = position, url = url)
    }
}

fun Game.toPlatformEntities(): List<PlatformEntity> {
    return platforms.map { it.toEntity() }
}

/**
 * One cross-ref per platform. When IGDB gave no per-platform date - every game saved straight from search
 * or Discover, whose queries don't request `release_dates` - the game's own first release date stands in
 * for all of them, so the row lands in a real Radar bucket instead of TBA. The periodic refresh replaces
 * it with the precise per-platform dates.
 *
 * The fallback's precision defaults to [EXACT_DATE], except when [releaseDate] lands on 31 December -
 * IGDB's placeholder for "only the year is known" (see [DateUtils.isYearOnlyPlaceholder]) - in which case
 * it is [com.example.gameswishlist.core.model.DatePrecision.YEAR_ONLY] instead, so the day doesn't show up
 * as if it were real.
 */
fun Game.toGamePlatformCrossRefs(): List<GamePlatformCrossRef> {
    val datesByPlatformId = releaseDates.associateBy { it.platformId }
    val fallbackDate = DateUtils.isoDateToEpochSeconds(releaseDate)
    val fallbackPrecision = if (DateUtils.isYearOnlyPlaceholder(releaseDate)) YEAR_ONLY else EXACT_DATE

    return platforms.map { platform ->
        val platformDate = datesByPlatformId[platform.id]
        GamePlatformCrossRef(
            gameId = id,
            platformId = platform.id,
            releaseDate = platformDate?.date ?: fallbackDate,
            releaseDatePrecision = platformDate?.precision?.toIgdbDateFormat()
                ?: fallbackDate?.let { fallbackPrecision.toIgdbDateFormat() }
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

/**
 * Reads a precision scalar back off a stored cross-ref row, where a missing value cannot be taken at face
 * value: rows written while the app still asked IGDB for the removed `category` field carry either no
 * scalar at all or the [EXACT_DATE] this mapper used to default to, both of which claim a precision IGDB
 * never gave. For those, a date landing on IGDB's 31 December year-only placeholder is read as
 * [YEAR_ONLY] rather than as a real day. Any other scalar was written from a value IGDB did return, so it
 * is trusted as-is.
 *
 * The trade is that a game genuinely released on 31 December reads as year-only too. That is the same
 * ambiguity [DateUtils.isYearOnlyPlaceholder] already accepts elsewhere, and it only applies to rows whose
 * precision is unknown — the periodic release-date refresh replaces those with a real `date_format`.
 */
private fun storedPrecision(rawScalar: Int?, date: Long?): DatePrecision {
    val precisionIsUnknown = rawScalar == null || rawScalar == EXACT_DATE.toIgdbDateFormat()
    if (precisionIsUnknown && DateUtils.isYearOnlyPlaceholder(date)) return YEAR_ONLY
    return fromIgdbDateFormat(rawScalar)
}

/** [dateFormat] is the raw IGDB `release_dates.date_format` value; an unrecognized or missing one defaults to [EXACT_DATE]. */
fun fromIgdbDateFormat(dateFormat: Int?): DatePrecision = when (dateFormat) {
    0 -> EXACT_DATE
    1 -> YEAR_MONTH
    2 -> YEAR_ONLY
    3, 4, 5, 6 -> QUARTER
    7 -> TBD
    else -> EXACT_DATE
}

/**
 * Reverse of [fromIgdbDateFormat], for persisting a domain-level [DatePrecision] back
 * as the raw IGDB scalar column. Lossy for [DatePrecision.QUARTER], which collapses IGDB's four quarter
 * values into one — round-trips back to [DatePrecision.QUARTER] regardless of which one.
 */
fun DatePrecision.toIgdbDateFormat(): Int = when (this) {
    EXACT_DATE -> 0
    YEAR_MONTH -> 1
    YEAR_ONLY -> 2
    QUARTER -> 3
    TBD -> 7
}