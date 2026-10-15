package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.data.backup.BACKUP_FORMAT_VERSION
import com.nikolasguillen.questlog.core.data.backup.BackupUnreadableException
import com.nikolasguillen.questlog.core.database.entity.CompanyEntity
import com.nikolasguillen.questlog.core.database.entity.EngineEntity
import com.nikolasguillen.questlog.core.database.entity.GameArtworkEntity
import com.nikolasguillen.questlog.core.database.entity.GameCompanyCrossRef
import com.nikolasguillen.questlog.core.database.entity.GameEntity
import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.database.entity.GamePlatformCrossRef
import com.nikolasguillen.questlog.core.database.entity.GenreEntity
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.entity.PlatformEntity
import com.nikolasguillen.questlog.core.database.entity.RelatedGameEntity
import com.nikolasguillen.questlog.core.database.relation.GameCompanyWithDetails
import com.nikolasguillen.questlog.core.database.relation.GamePlatformWithDetails
import com.nikolasguillen.questlog.core.database.relation.GameWithAllDetails
import com.nikolasguillen.questlog.core.database.userdata.UserDataRows
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.WishlistIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class BackupMapperTest {

    private val platform = PlatformEntity(
        id = 130, name = "Nintendo Switch", abbreviation = "Switch", generation = 8, category = 5, platformFamily = 5
    )

    private val game = GameWithAllDetails(
        game = GameEntity(
            id = 119133,
            name = "Elden Ring",
            description = "An action RPG.",
            released = "2022-02-25",
            backgroundImage = "https://img/er.jpg",
            rating = 94.5,
            ratingCount = 1200,
            hypes = 40,
            metacritic = 96,
            gameTypeId = 0,
            notes = "Try the bow build",
            priority = 2,
            status = GameStatus.PLAYING,
            url = "https://igdb/er",
            lastViewedAt = 1_700_000_000L,
            detailsFetchedAt = 1_700_000_100L
        ),
        platformRefs = listOf(
            GamePlatformWithDetails(
                crossRef = GamePlatformCrossRef(gameId = 119133, platformId = 130, releaseDate = 1_645_747_200L, releaseDatePrecision = 2),
                platform = platform
            )
        ),
        genres = listOf(GenreEntity(id = 12, name = "RPG")),
        companyRefs = listOf(
            GameCompanyWithDetails(
                crossRef = GameCompanyCrossRef(gameId = 119133, companyId = 70, isDeveloper = true, isPublisher = false),
                company = CompanyEntity(id = 70, name = "FromSoftware")
            )
        ),
        relatedGames = listOf(
            RelatedGameEntity(parentId = 119133, relatedGameId = 1, name = "DLC", coverUrl = "https://img/dlc.jpg", relationType = "dlc")
        ),
        engines = listOf(EngineEntity(id = 9, name = "In-house")),
        artworks = listOf(
            GameArtworkEntity(gameId = 119133, position = 1, url = "https://img/2.jpg"),
            GameArtworkEntity(gameId = 119133, position = 0, url = "https://img/1.jpg")
        )
    )

    private val rows = UserDataRows(
        lists = listOf(
            ListEntity(id = 1, name = "Wishlist", description = "Default", icon = WishlistIcon.HEART, coverImagePath = null),
            ListEntity(id = 2, name = "Co-op", description = "With friends", icon = WishlistIcon.MULTIPLAYER, coverImagePath = "/covers/a.jpg")
        ),
        defaultListId = 2,
        memberships = listOf(GameListCrossRef(gameId = 119133, listId = 2)),
        ownedPlatformIds = listOf(130),
        ownedPlatforms = listOf(platform),
        savedGames = listOf(game)
    )

    private val createdAt = Instant.fromEpochMilliseconds(1_750_000_000_000L)

    private fun snapshot(coverListIds: Set<Long> = setOf(2L)) =
        rows.toBackupSnapshot(createdAt, appVersion = "1.0", coverListIds = coverListIds)

    @Test
    fun `the snapshot carries the header and the default list`() {
        val snapshot = snapshot()

        assertEquals(BACKUP_FORMAT_VERSION, snapshot.formatVersion)
        assertEquals(1_750_000_000_000L, snapshot.createdAtEpochMillis)
        assertEquals("1.0", snapshot.appVersion)
        assertEquals(2L, snapshot.defaultListId)
    }

    @Test
    fun `lists keep their icon and say whether a cover was readable`() {
        val lists = snapshot().lists

        assertEquals("HEART", lists[0].icon)
        assertFalse(lists[0].hasCover)
        assertEquals("MULTIPLAYER", lists[1].icon)
        assertTrue(lists[1].hasCover)
        assertEquals("With friends", lists[1].description)
    }

    @Test
    fun `a list whose cover file is missing is written without a cover`() {
        val lists = snapshot(coverListIds = emptySet()).lists

        assertFalse(lists[1].hasCover)
    }

    @Test
    fun `a game carries every field and every child collection`() {
        val backup = snapshot().games.single()

        assertEquals(119133, backup.id)
        assertEquals("Elden Ring", backup.name)
        assertEquals("An action RPG.", backup.description)
        assertEquals("2022-02-25", backup.released)
        assertEquals("https://img/er.jpg", backup.backgroundImage)
        assertEquals(94.5, backup.rating, 0.0)
        assertEquals(1200, backup.ratingCount)
        assertEquals(40, backup.hypes)
        assertEquals(96, backup.metacritic)
        assertEquals(0, backup.gameTypeId)
        assertEquals("Try the bow build", backup.notes)
        assertEquals(2, backup.priority)
        assertEquals("PLAYING", backup.status)
        assertEquals("https://igdb/er", backup.url)
        assertEquals(1_700_000_100L, backup.detailsFetchedAt)

        val platformBackup = backup.platforms.single()
        assertEquals(130, platformBackup.id)
        assertEquals("Switch", platformBackup.abbreviation)
        assertEquals(1_645_747_200L, platformBackup.releaseDate)
        assertEquals(2, platformBackup.releaseDatePrecision)

        assertEquals("RPG", backup.genres.single().name)
        val company = backup.companies.single()
        assertEquals("FromSoftware", company.name)
        assertTrue(company.isDeveloper)
        assertFalse(company.isPublisher)
        assertEquals("In-house", backup.engines.single().name)
        assertEquals("dlc", backup.relatedGames.single().relationType)
    }

    @Test
    fun `artworks keep the gallery order`() {
        assertEquals(listOf("https://img/1.jpg", "https://img/2.jpg"), snapshot().games.single().artworks.map { it.url })
    }

    @Test
    fun `memberships and owned platforms are carried`() {
        val snapshot = snapshot()

        assertEquals(119133, snapshot.memberships.single().gameId)
        assertEquals(2L, snapshot.memberships.single().listId)
        assertEquals(listOf(130), snapshot.ownedPlatformIds)
        assertEquals("Nintendo Switch", snapshot.ownedPlatforms.single().name)
    }

    @Test
    fun `last viewed is not part of the snapshot`() {
        // BackupGame has no field for it, so the proof is the serialized form.
        val json = kotlinx.serialization.json.Json.encodeToString(
            com.nikolasguillen.questlog.core.data.backup.model.BackupSnapshot.serializer(),
            snapshot()
        )

        assertFalse(json.contains("lastViewedAt"))
        assertFalse(json.contains("1700000000"))
    }

    @Test
    fun `a status or an icon this build does not know reads back as null`() {
        assertEquals(GameStatus.COMPLETED, "COMPLETED".toGameStatusOrNull())
        assertNull("SPEEDRUNNING".toGameStatusOrNull())
        assertNull(null.toGameStatusOrNull())
        assertEquals(WishlistIcon.BACKLOG, "BACKLOG".toWishlistIconOrNull())
        assertNull("ROCKET".toWishlistIconOrNull())
        assertNull(null.toWishlistIconOrNull())
    }

    @Test
    fun `the inverse mapping restores every row except last viewed and cover paths`() {
        val restored = snapshot().toUserDataRows(coverPaths = mapOf(2L to "/covers/new.jpg"))

        val expected = rows.copy(
            lists = listOf(rows.lists[0], rows.lists[1].copy(coverImagePath = "/covers/new.jpg")),
            // The snapshot keeps the gallery order, and the device keeps its own last viewed.
            savedGames = listOf(
                game.copy(
                    game = game.game.copy(lastViewedAt = null),
                    artworks = game.artworks.sortedBy { it.position }
                )
            )
        )
        assertEquals(expected, restored)
    }

    @Test
    fun `a list with a cover takes the supplied path and one without gets none`() {
        val restored = snapshot().toUserDataRows(coverPaths = mapOf(1L to "/covers/ignored.jpg", 2L to "/covers/b.jpg"))

        assertNull(restored.lists[0].coverImagePath)
        assertEquals("/covers/b.jpg", restored.lists[1].coverImagePath)
    }

    @Test
    fun `a snapshot whose default list is not among its lists is unreadable`() {
        val broken = snapshot().copy(defaultListId = 99)

        assertThrows(BackupUnreadableException::class.java) { broken.toUserDataRows(emptyMap()) }
    }

    @Test
    fun `a membership of a list the snapshot does not hold is dropped`() {
        val withOrphan = snapshot().copy(
            memberships = snapshot().memberships + com.nikolasguillen.questlog.core.data.backup.model.BackupMembership(7, 42)
        )

        assertEquals(listOf(GameListCrossRef(119133, 2)), withOrphan.toUserDataRows(emptyMap()).memberships)
    }

    @Test
    fun `unknown status and icon names in a snapshot read back as null`() {
        val snapshot = snapshot()
        val newer = snapshot.copy(
            lists = snapshot.lists.map { it.copy(icon = "ROCKET") },
            games = snapshot.games.map { it.copy(status = "SPEEDRUNNING") }
        )

        val restored = newer.toUserDataRows(emptyMap())

        assertNull(restored.lists[0].icon)
        assertNull(restored.savedGames.single().game.status)
    }
}
