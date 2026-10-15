package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.data.backup.BACKUP_FORMAT_VERSION
import com.nikolasguillen.questlog.core.data.backup.BackupUnreadableException
import com.nikolasguillen.questlog.core.data.backup.model.BackupArtwork
import com.nikolasguillen.questlog.core.data.backup.model.BackupCompany
import com.nikolasguillen.questlog.core.data.backup.model.BackupEngine
import com.nikolasguillen.questlog.core.data.backup.model.BackupGame
import com.nikolasguillen.questlog.core.data.backup.model.BackupGamePlatform
import com.nikolasguillen.questlog.core.data.backup.model.BackupGenre
import com.nikolasguillen.questlog.core.data.backup.model.BackupList
import com.nikolasguillen.questlog.core.data.backup.model.BackupMembership
import com.nikolasguillen.questlog.core.data.backup.model.BackupPlatform
import com.nikolasguillen.questlog.core.data.backup.model.BackupRelatedGame
import com.nikolasguillen.questlog.core.data.backup.model.BackupSnapshot
import com.nikolasguillen.questlog.core.database.entity.CompanyEntity
import com.nikolasguillen.questlog.core.database.entity.EngineEntity
import com.nikolasguillen.questlog.core.database.entity.GameArtworkEntity
import com.nikolasguillen.questlog.core.database.entity.GameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameEntity
import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.database.entity.GamePlatformCrossRef
import com.nikolasguillen.questlog.core.database.entity.GenreEntity
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.entity.RelatedGameEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.relation.GameCompanyWithDetails
import com.nikolasguillen.questlog.core.database.relation.GamePlatformWithDetails
import com.nikolasguillen.questlog.core.database.relation.GameWithAllDetails
import com.nikolasguillen.questlog.core.database.userdata.UserDataRows
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.WishlistIcon
import kotlin.time.Instant

/**
 * The device's user data as a backup snapshot. [coverListIds] are the lists whose cover file could be read, which is
 * what `hasCover` says: a list whose cover file went missing is written without one rather than pointing at nothing.
 *
 * `lastViewedAt` is deliberately left out: the Recently viewed history is not part of the backup.
 */
internal fun UserDataRows.toBackupSnapshot(
    createdAt: Instant,
    appVersion: String,
    coverListIds: Set<Long>
): BackupSnapshot = BackupSnapshot(
    formatVersion = BACKUP_FORMAT_VERSION,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    appVersion = appVersion,
    lists = lists.map { it.toBackupList(hasCover = it.id in coverListIds) },
    defaultListId = defaultListId,
    games = savedGames.map { it.toBackupGame() },
    memberships = memberships.map { BackupMembership(gameId = it.gameId, listId = it.listId) },
    ownedPlatformIds = ownedPlatformIds,
    ownedPlatforms = ownedPlatforms.map { it.toBackupPlatform() }
)

private fun ListEntity.toBackupList(hasCover: Boolean) = BackupList(
    id = id,
    name = name,
    description = description,
    icon = icon?.name,
    hasCover = hasCover
)

private fun PlatformEntity.toBackupPlatform() = BackupPlatform(
    id = id,
    name = name,
    abbreviation = abbreviation,
    generation = generation,
    category = category,
    platformFamily = platformFamily
)

private fun GameWithAllDetails.toBackupGame() = BackupGame(
    id = game.id,
    name = game.name,
    description = game.description,
    released = game.released,
    backgroundImage = game.backgroundImage,
    rating = game.rating,
    ratingCount = game.ratingCount,
    hypes = game.hypes,
    metacritic = game.metacritic,
    gameTypeId = game.gameTypeId,
    notes = game.notes,
    priority = game.priority,
    status = game.status?.name,
    url = game.url,
    detailsFetchedAt = game.detailsFetchedAt,
    platforms = platformRefs.map {
        BackupGamePlatform(
            id = it.platform.id,
            name = it.platform.name,
            abbreviation = it.platform.abbreviation,
            generation = it.platform.generation,
            category = it.platform.category,
            platformFamily = it.platform.platformFamily,
            releaseDate = it.crossRef.releaseDate,
            releaseDatePrecision = it.crossRef.releaseDatePrecision
        )
    },
    genres = genres.map { BackupGenre(id = it.id, name = it.name) },
    companies = companyRefs.map {
        BackupCompany(
            id = it.company.id,
            name = it.company.name,
            isDeveloper = it.crossRef.isDeveloper,
            isPublisher = it.crossRef.isPublisher
        )
    },
    engines = engines.map { BackupEngine(id = it.id, name = it.name) },
    artworks = artworks.sortedBy { it.position }.map { BackupArtwork(position = it.position, url = it.url) },
    relatedGames = relatedGames.map {
        BackupRelatedGame(
            relatedGameId = it.relatedGameId,
            name = it.name,
            coverUrl = it.coverUrl,
            relationType = it.relationType
        )
    }
)

/**
 * The inverse of [toBackupSnapshot]. [coverPaths] holds, per list id, the file a cover was imported to on this device; a
 * list with `hasCover` takes it and any other list has none. `lastViewedAt` is left empty: the device keeps its own.
 *
 * Throws [BackupUnreadableException] when the default list is not among the lists, because a pointer to nothing would
 * leave the app without a default wishlist. A membership of a list the snapshot does not hold is dropped: it points at
 * nothing the user can see.
 */
internal fun BackupSnapshot.toUserDataRows(coverPaths: Map<Long, String>): UserDataRows {
    if (lists.none { it.id == defaultListId }) {
        throw BackupUnreadableException("The default list $defaultListId is not among the backup's lists")
    }
    val listIds = lists.mapTo(mutableSetOf()) { it.id }
    return UserDataRows(
        lists = lists.map {
            ListEntity(
                id = it.id,
                name = it.name,
                description = it.description,
                icon = it.icon.toWishlistIconOrNull(),
                coverImagePath = if (it.hasCover) coverPaths[it.id] else null
            )
        },
        defaultListId = defaultListId,
        memberships = memberships.filter { it.listId in listIds }.map { GameListCrossRef(gameId = it.gameId, listId = it.listId) },
        ownedPlatformIds = ownedPlatformIds,
        ownedPlatforms = ownedPlatforms.map {
            PlatformEntity(
                id = it.id, name = it.name, abbreviation = it.abbreviation, generation = it.generation,
                category = it.category, platformFamily = it.platformFamily
            )
        },
        savedGames = games.map { it.toGameWithAllDetails() }
    )
}

private fun BackupGame.toGameWithAllDetails() = GameWithAllDetails(
    game = GameEntity(
        id = id,
        name = name,
        description = description,
        released = released,
        backgroundImage = backgroundImage,
        rating = rating,
        ratingCount = ratingCount,
        hypes = hypes,
        metacritic = metacritic,
        gameTypeId = gameTypeId,
        notes = notes,
        priority = priority,
        status = status.toGameStatusOrNull(),
        url = url,
        lastViewedAt = null,
        detailsFetchedAt = detailsFetchedAt
    ),
    platformRefs = platforms.map {
        GamePlatformWithDetails(
            crossRef = GamePlatformCrossRef(
                gameId = id, platformId = it.id, releaseDate = it.releaseDate, releaseDatePrecision = it.releaseDatePrecision
            ),
            platform = PlatformEntity(
                id = it.id, name = it.name, abbreviation = it.abbreviation, generation = it.generation,
                category = it.category, platformFamily = it.platformFamily
            )
        )
    },
    genres = genres.map { GenreEntity(id = it.id, name = it.name) },
    companyRefs = companies.map {
        GameCompanyWithDetails(
            crossRef = GameCompanyCrossRef(
                gameId = id, companyId = it.id, isDeveloper = it.isDeveloper, isPublisher = it.isPublisher
            ),
            company = CompanyEntity(id = it.id, name = it.name)
        )
    },
    relatedGames = relatedGames.map {
        RelatedGameEntity(
            parentId = id, relatedGameId = it.relatedGameId, name = it.name,
            coverUrl = it.coverUrl, relationType = it.relationType
        )
    },
    engines = engines.map { EngineEntity(id = it.id, name = it.name) },
    artworks = artworks.map { GameArtworkEntity(gameId = id, position = it.position, url = it.url) }
)

/** A status name written by a newer build, which this one does not know, reads back as no status. */
internal fun String?.toGameStatusOrNull(): GameStatus? = this?.let { name -> GameStatus.entries.firstOrNull { it.name == name } }

/** An icon name this build does not know reads back as no icon. */
internal fun String?.toWishlistIconOrNull(): WishlistIcon? = this?.let { name -> WishlistIcon.entries.firstOrNull { it.name == name } }
