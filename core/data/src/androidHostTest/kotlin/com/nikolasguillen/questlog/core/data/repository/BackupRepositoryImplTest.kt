package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.CloudProvider
import com.nikolasguillen.questlog.core.cloud.model.CloudUser
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupStreamDocument
import com.nikolasguillen.questlog.core.common.AppVersionProvider
import com.nikolasguillen.questlog.core.data.backup.BACKUP_FORMAT_VERSION
import com.nikolasguillen.questlog.core.data.backup.MAX_BACKUP_PARTS
import com.nikolasguillen.questlog.core.data.backup.joinParts
import com.nikolasguillen.questlog.core.data.backup.model.BackupSnapshot
import com.nikolasguillen.questlog.core.data.cloud.FakeCloudAuth
import com.nikolasguillen.questlog.core.data.cloud.FakeCloudBackupStore
import com.nikolasguillen.questlog.core.data.local.FakeWishlistCoverImageStorage
import com.nikolasguillen.questlog.core.database.dao.UserDataDao
import com.nikolasguillen.questlog.core.database.entity.GameEntity
import com.nikolasguillen.questlog.core.database.entity.GameListCrossRef
import com.nikolasguillen.questlog.core.database.entity.ListEntity
import com.nikolasguillen.questlog.core.database.relation.GameWithAllDetails
import com.nikolasguillen.questlog.core.database.userdata.UserDataRows
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.BackupInfo
import com.nikolasguillen.questlog.core.model.GameStatus
import com.nikolasguillen.questlog.core.model.GameStatus.COMPLETED
import com.nikolasguillen.questlog.core.model.RepositoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

class BackupRepositoryImplTest {

    private val signedIn = CloudUser(uid = "uid-1", email = "ada@example.com", providers = setOf(CloudProvider.GOOGLE))
    private val auth = FakeCloudAuth(initial = signedIn)
    private val store = FakeCloudBackupStore()
    private val covers = FakeWishlistCoverImageStorage()
    private val dao = mockk<UserDataDao>()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
    private val now = Instant.fromEpochMilliseconds(1_750_000_000_000L)

    private val rows = UserDataRows(
        lists = listOf(
            ListEntity(id = 1, name = "Wishlist", description = "Default"),
            ListEntity(id = 2, name = "Co-op", description = "With friends", coverImagePath = "/covers/a.jpg")
        ),
        defaultListId = 1,
        memberships = listOf(GameListCrossRef(gameId = 7, listId = 2)),
        ownedPlatformIds = emptyList(),
        ownedPlatforms = emptyList(),
        savedGames = listOf(
            GameWithAllDetails(
                game = GameEntity(
                    id = 7, name = "Hades", description = "Roguelike", released = null, backgroundImage = null,
                    rating = 90.0, metacritic = null, gameTypeId = 0, notes = "", priority = null,
                    status = GameStatus.COMPLETED, url = null
                ),
                platformRefs = emptyList(), genres = emptyList(), companyRefs = emptyList(),
                relatedGames = emptyList(), engines = emptyList(), artworks = emptyList()
            )
        )
    )

    private fun repository() = BackupRepositoryImpl(
        cloudAuth = auth,
        cloudBackupStore = store,
        userDataDao = dao,
        coverStorage = covers,
        appVersionProvider = object : AppVersionProvider { override val versionName = "1.0" },
        clock = object : Clock { override fun now() = this@BackupRepositoryImplTest.now },
        json = json
    )

    private fun givenRows() {
        coEvery { dao.readUserData() } returns rows
        covers.files["/covers/a.jpg"] = byteArrayOf(1, 2, 3, 4)
    }

    @Test
    fun `every part is written before the manifest and the manifest is written last`() = runTest {
        givenRows()

        val result = repository().backUp()

        assertTrue(result is AppResult.Success)
        val calls = store.calls
        val manifestAt = calls.indexOf("writeManifest")
        assertTrue(manifestAt > 0)
        assertEquals(2, calls.count { it.startsWith("writePart:") }) // the data stream and one cover
        assertTrue(calls.withIndex().filter { it.value.startsWith("writePart:") }.all { it.index < manifestAt })
        assertEquals("readManifest", calls.first())
        // Only the best-effort sweep may follow the commit point.
        assertTrue(calls.drop(manifestAt + 1).all { it == "listPartIds" || it == "deleteParts" })
    }

    @Test
    fun `the manifest describes the streams and the parts hold the snapshot and the cover`() = runTest {
        givenRows()

        val info = (repository().backUp() as AppResult.Success).data

        val manifest = store.manifests.getValue("uid-1")
        assertEquals(BACKUP_FORMAT_VERSION, manifest.formatVersion)
        assertEquals(2, manifest.listCount)
        assertEquals(1, manifest.gameCount)
        assertEquals(BackupInfo(createdAt = now, listCount = 2, gameCount = 1), info)
        assertEquals(listOf("data", "cover-2"), manifest.streams.map { it.name })

        val dataStream = manifest.streams.first()
        val snapshot = json.decodeFromString(
            BackupSnapshot.serializer(),
            dataStream.partIds.map { store.parts.getValue(it).data }
                .joinParts(dataStream.byteLength).decodeToString()
        )
        assertEquals(listOf(1L, 2L), snapshot.lists.map { it.id })
        assertTrue(snapshot.lists[1].hasCover)
        assertEquals("COMPLETED", snapshot.games.single().status)

        val coverStream = manifest.streams[1]
        val coverBytes = coverStream.partIds.map { store.parts.getValue(it).data }.joinParts(coverStream.byteLength)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), coverBytes)
    }

    @Test
    fun `a failed part write leaves the manifest unwritten and reports the mapped error`() = runTest {
        givenRows()
        store.failWriteNumber = 2

        val result = repository().backUp()

        assertEquals(RepositoryError.NoNetwork, (result as AppResult.Failure).error)
        assertFalse("writeManifest" in store.calls)
        assertNull(store.manifests["uid-1"])
    }

    @Test
    fun `parts the new manifest does not list are deleted after the commit`() = runTest {
        givenRows()
        store.parts["old-data-0"] = BackupPartDocument("old", "data", 0, "AAAA")

        repository().backUp()

        assertFalse("old-data-0" in store.parts)
        assertEquals(2, store.parts.size)
        assertTrue("deleteParts" in store.calls)
    }

    @Test
    fun `a failing sweep still returns success`() = runTest {
        givenRows()
        store.parts["old-data-0"] = BackupPartDocument("old", "data", 0, "AAAA")
        store.failDeleteParts = true

        val result = repository().backUp()

        assertTrue(result is AppResult.Success)
        assertTrue("writeManifest" in store.calls)
    }

    @Test
    fun `a collection that needs more than the maximum number of parts is refused before anything is written`() = runTest {
        coEvery { dao.readUserData() } returns rows
        // 21 MB of cover is about 28 million base64 characters: 41 parts, one more than a backup may have.
        covers.files["/covers/a.jpg"] = ByteArray(21_000_000)

        val result = repository().backUp()

        assertEquals(RepositoryError.BackupTooLarge, (result as AppResult.Failure).error)
        assertTrue(MAX_BACKUP_PARTS < 42)
        assertTrue(store.calls.none { it.startsWith("writePart") || it == "writeManifest" })
    }

    @Test
    fun `signed out returns not signed in without touching the store`() = runTest {
        auth.user.value = null

        val backUp = repository().backUp()
        val info = repository().getBackupInfo()

        assertEquals(RepositoryError.NotSignedIn, (backUp as AppResult.Failure).error)
        assertEquals(RepositoryError.NotSignedIn, (info as AppResult.Failure).error)
        assertTrue(store.calls.isEmpty())
    }

    @Test
    fun `offline fails at the first read and writes nothing`() = runTest {
        givenRows()
        store.readManifestFailure = CloudException.Connectivity()

        val result = repository().backUp()

        assertEquals(RepositoryError.NoNetwork, (result as AppResult.Failure).error)
        assertEquals(listOf("readManifest"), store.calls)
    }

    @Test
    fun `getBackupInfo is null when there is no manifest`() = runTest {
        val result = repository().getBackupInfo()

        assertNull((result as AppResult.Success).data)
    }

    @Test
    fun `getBackupInfo maps the manifest`() = runTest {
        store.manifests["uid-1"] = BackupManifestDocument(
            snapshotId = "s", formatVersion = 1, createdAtEpochMillis = 1_750_000_000_000L, appVersion = "1.0",
            listCount = 3, gameCount = 20, streams = listOf(BackupStreamDocument("data", 0, emptyList()))
        )

        val result = repository().getBackupInfo()

        assertEquals(BackupInfo(createdAt = now, listCount = 3, gameCount = 20), (result as AppResult.Success).data)
    }

    @Test
    fun `hasLocalUserData delegates to the dao`() = runTest {
        coEvery { dao.hasUserData() } returns true
        assertTrue(repository().hasLocalUserData())

        coEvery { dao.hasUserData() } returns false
        assertFalse(repository().hasLocalUserData())
    }

    @Test
    fun `two concurrent backups run one after the other`() = runTest {
        givenRows()
        store.readManifestDelayMillis = 100
        val repository = repository()

        launch { repository.backUp() }
        launch { repository.backUp() }
        advanceUntilIdle()

        assertEquals(2, store.calls.count { it == "readManifest" })
        assertEquals(1, store.maxInFlightReads)
        assertEquals(2, store.calls.count { it == "writeManifest" })
    }

    // ---- restore ----

    /** A second device: its own database and cover files, sharing only the cloud store with the one that backed up. */
    private inner class RestoringDevice {
        val covers = FakeWishlistCoverImageStorage()
        val dao = mockk<UserDataDao>()
        val replaced = slot<UserDataRows>()
        val repository = BackupRepositoryImpl(
            cloudAuth = auth,
            cloudBackupStore = store,
            userDataDao = dao,
            coverStorage = covers,
            appVersionProvider = object : AppVersionProvider { override val versionName = "1.0" },
            clock = object : Clock { override fun now() = this@BackupRepositoryImplTest.now },
            json = json
        )

        init {
            coEvery { dao.readCoverPaths() } returns emptyList()
            coEvery { dao.replaceUserData(capture(replaced)) } just runs
        }
    }

    private suspend fun backUpFirst() {
        givenRows()
        repository().backUp()
        store.calls.clear()
    }

    @Test
    fun `restore without a backup returns not found and reads no part`() = runTest {
        val device = RestoringDevice()

        val result = device.repository.restore()

        assertEquals(RepositoryError.BackupNotFound, (result as AppResult.Failure).error)
        assertTrue(store.calls.none { it.startsWith("readPart") })
        coVerify(exactly = 0) { device.dao.replaceUserData(any()) }
    }

    @Test
    fun `restore of a newer format is refused before any part is read`() = runTest {
        backUpFirst()
        store.manifests["uid-1"] = store.manifests.getValue("uid-1").copy(formatVersion = BACKUP_FORMAT_VERSION + 1)
        val device = RestoringDevice()

        val result = device.repository.restore()

        assertEquals(RepositoryError.BackupFromNewerVersion, (result as AppResult.Failure).error)
        assertTrue(store.calls.none { it.startsWith("readPart") })
        coVerify(exactly = 0) { device.dao.replaceUserData(any()) }
    }

    @Test
    fun `a missing part is unreadable and changes nothing`() = runTest {
        backUpFirst()
        val cover = store.manifests.getValue("uid-1").streams.last()
        store.parts.remove(cover.partIds.first())
        val device = RestoringDevice()

        val result = device.repository.restore()

        assertEquals(RepositoryError.BackupUnreadable, (result as AppResult.Failure).error)
        coVerify(exactly = 0) { device.dao.replaceUserData(any()) }
        assertTrue(device.covers.files.isEmpty())
    }

    @Test
    fun `a stream whose length differs from the manifest is unreadable`() = runTest {
        backUpFirst()
        val manifest = store.manifests.getValue("uid-1")
        store.manifests["uid-1"] = manifest.copy(
            streams = manifest.streams.map { if (it.name == "data") it.copy(byteLength = it.byteLength + 1) else it }
        )
        val device = RestoringDevice()

        val result = device.repository.restore()

        assertEquals(RepositoryError.BackupUnreadable, (result as AppResult.Failure).error)
        coVerify(exactly = 0) { device.dao.replaceUserData(any()) }
    }

    @Test
    fun `a part from another snapshot is unreadable`() = runTest {
        backUpFirst()
        val partId = store.manifests.getValue("uid-1").streams.first().partIds.first()
        store.parts[partId] = store.parts.getValue(partId).copy(snapshotId = "stale")
        val device = RestoringDevice()

        val result = device.repository.restore()

        assertEquals(RepositoryError.BackupUnreadable, (result as AppResult.Failure).error)
    }

    @Test
    fun `a failing transaction deletes the imported covers and returns the error`() = runTest {
        backUpFirst()
        val device = RestoringDevice()
        coEvery { device.dao.replaceUserData(any()) } throws IllegalStateException("disk full")

        val result = device.repository.restore()

        assertTrue((result as AppResult.Failure).error is RepositoryError.Unknown)
        assertEquals(1, device.covers.imported.size)
        assertTrue(device.covers.files.isEmpty())
    }

    @Test
    fun `restore replaces the data, sweeps first and deletes the old covers last`() = runTest {
        backUpFirst()
        val device = RestoringDevice()
        coEvery { device.dao.readCoverPaths() } returns listOf("/covers/old.jpg")
        device.covers.files["/covers/old.jpg"] = byteArrayOf(9)

        val result = device.repository.restore()

        assertEquals(BackupInfo(createdAt = now, listCount = 2, gameCount = 1), (result as AppResult.Success).data)
        val rows = device.replaced.captured
        assertEquals(listOf(1L, 2L), rows.lists.map { it.id })
        assertEquals(1L, rows.defaultListId)
        assertEquals(COMPLETED, rows.savedGames.single().game.status)
        val importedPath = device.covers.imported.single()
        assertEquals(importedPath, rows.lists[1].coverImagePath)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), device.covers.files.getValue(importedPath))

        val calls = device.covers.calls
        assertEquals("deleteAllExcept", calls.first())
        assertTrue(calls.indexOf("import") < calls.indexOf("delete:/covers/old.jpg"))
        assertFalse("/covers/old.jpg" in device.covers.files)
        assertTrue(importedPath in device.covers.files)
    }

    @Test
    fun `restore signed out returns not signed in without touching the store`() = runTest {
        auth.user.value = null
        val device = RestoringDevice()

        val result = device.repository.restore()

        assertEquals(RepositoryError.NotSignedIn, (result as AppResult.Failure).error)
        assertTrue(store.calls.isEmpty())
    }

    @Test
    fun `restore and backup never overlap`() = runTest {
        backUpFirst()
        store.readManifestDelayMillis = 100
        val device = RestoringDevice()
        coEvery { device.dao.readUserData() } returns rows

        // One repository, as the app has: its lock is what keeps the two operations apart.
        launch { device.repository.backUp() }
        launch { device.repository.restore() }
        advanceUntilIdle()

        assertEquals(1, store.maxInFlightReads)
        assertEquals(2, store.calls.count { it == "readManifest" })
    }

    @Test
    fun `deleteBackup deletes every part and then the manifest`() = runTest {
        backUpFirst()
        store.parts["stale-part"] = BackupPartDocument(snapshotId = "old", stream = "data", index = 0, data = "x")
        store.calls.clear()

        val result = repository().deleteBackup()

        assertEquals(AppResult.success(Unit), result)
        assertEquals(listOf("listPartIds", "deleteParts", "deleteManifest"), store.calls)
        assertTrue(store.parts.isEmpty())
        assertTrue(store.manifests.isEmpty())
    }

    @Test
    fun `a failing part delete stops before the manifest is deleted`() = runTest {
        backUpFirst()
        store.failDeleteParts = true

        val result = repository().deleteBackup()

        assertEquals(RepositoryError.NoNetwork, (result as AppResult.Failure).error)
        assertFalse("deleteManifest" in store.calls)
        assertEquals(setOf("uid-1"), store.manifests.keys)
    }

    @Test
    fun `deleteBackup with no backup still succeeds`() = runTest {
        val result = repository().deleteBackup()

        assertEquals(AppResult.success(Unit), result)
        assertEquals(listOf("listPartIds", "deleteManifest"), store.calls)
    }

    @Test
    fun `deleteBackup signed out returns not signed in without touching the store`() = runTest {
        auth.user.value = null

        val result = repository().deleteBackup()

        assertEquals(RepositoryError.NotSignedIn, (result as AppResult.Failure).error)
        assertTrue(store.calls.isEmpty())
    }
}
