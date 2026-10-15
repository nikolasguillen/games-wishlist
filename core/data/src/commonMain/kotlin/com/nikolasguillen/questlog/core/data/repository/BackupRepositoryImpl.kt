package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupStreamDocument
import com.nikolasguillen.questlog.core.common.AppVersionProvider
import com.nikolasguillen.questlog.core.data.backup.BACKUP_FORMAT_VERSION
import com.nikolasguillen.questlog.core.data.backup.MAX_BACKUP_PARTS
import com.nikolasguillen.questlog.core.data.backup.model.BackupSnapshot
import com.nikolasguillen.questlog.core.data.backup.toParts
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.mapper.toBackupSnapshot
import com.nikolasguillen.questlog.core.database.dao.UserDataDao
import com.nikolasguillen.questlog.core.domain.repository.BackupRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.BackupInfo
import com.nikolasguillen.questlog.core.model.RepositoryError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/** The name of the stream that holds the snapshot JSON. Cover photos travel in `cover-<listId>`. */
private const val DATA_STREAM = "data"

internal class BackupRepositoryImpl(
    private val cloudAuth: CloudAuth,
    private val cloudBackupStore: CloudBackupStore,
    private val userDataDao: UserDataDao,
    private val coverStorage: WishlistCoverImageStorage,
    private val appVersionProvider: AppVersionProvider,
    private val clock: Clock,
    private val json: Json
) : BackupRepository {

    // Backing up, restoring and deleting must never overlap: each one reads or rewrites the same documents.
    private val operationLock = Mutex()

    override suspend fun getBackupInfo(): AppResult<BackupInfo?> = try {
        val uid = currentUid() ?: return AppResult.failure(RepositoryError.NotSignedIn)
        AppResult.success(cloudBackupStore.readManifest(uid)?.toBackupInfo())
    } catch (e: Exception) {
        AppResult.failure(e.toRepositoryError())
    }

    override suspend fun backUp(): AppResult<BackupInfo> = operationLock.withLock {
        try {
            val uid = currentUid() ?: return@withLock AppResult.failure(RepositoryError.NotSignedIn)

            // Read from the server first, and for no other reason: offline this fails at once, instead of after the
            // snapshot has been built and the first write has hung.
            cloudBackupStore.readManifest(uid)

            val rows = userDataDao.readUserData()
            val covers = readCovers(rows.lists.mapNotNull { list -> list.coverImagePath?.let { list.id to it } })
            val snapshot = rows.toBackupSnapshot(
                createdAt = clock.now(),
                appVersion = appVersionProvider.versionName,
                coverListIds = covers.keys
            )

            val streams = buildList {
                add(DATA_STREAM to json.encodeToString(BackupSnapshot.serializer(), snapshot).encodeToByteArray())
                covers.forEach { (listId, bytes) -> add("cover-$listId" to bytes) }
            }
            val chunked = streams.map { (name, bytes) -> ChunkedStream(name, bytes.size.toLong(), bytes.toParts()) }
            if (chunked.sumOf { it.parts.size } > MAX_BACKUP_PARTS) {
                return@withLock AppResult.failure(RepositoryError.BackupTooLarge)
            }

            val snapshotId = Uuid.random().toString()
            val streamDocuments = chunked.map { stream ->
                BackupStreamDocument(
                    name = stream.name,
                    byteLength = stream.byteLength,
                    partIds = stream.parts.indices.map { index -> partId(snapshotId, stream.name, index) }
                )
            }
            chunked.zip(streamDocuments).forEach { (stream, document) ->
                stream.parts.forEachIndexed { index, text ->
                    cloudBackupStore.writePart(
                        uid = uid,
                        partId = document.partIds[index],
                        part = BackupPartDocument(snapshotId = snapshotId, stream = stream.name, index = index, data = text)
                    )
                }
            }

            val manifest = BackupManifestDocument(
                snapshotId = snapshotId,
                formatVersion = BACKUP_FORMAT_VERSION,
                createdAtEpochMillis = snapshot.createdAtEpochMillis,
                appVersion = snapshot.appVersion,
                listCount = snapshot.lists.size,
                gameCount = snapshot.games.size,
                streams = streamDocuments
            )
            // The commit point: until this returns, the previous manifest still points at the previous, untouched parts.
            cloudBackupStore.writeManifest(uid, manifest)

            sweepStaleParts(uid, keep = streamDocuments.flatMap { it.partIds }.toSet())
            AppResult.success(manifest.toBackupInfo())
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        }
    }

    override suspend fun hasLocalUserData(): Boolean = userDataDao.hasUserData()

    private suspend fun currentUid(): String? = cloudAuth.currentUser.first()?.uid

    /** The bytes of every cover that can be read. A list whose file is gone simply has no cover in the backup. */
    private suspend fun readCovers(coverPaths: List<Pair<Long, String>>): Map<Long, ByteArray> =
        buildMap {
            coverPaths.forEach { (listId, path) ->
                coverStorage.read(path)?.takeIf { it.isNotEmpty() }?.let { put(listId, it) }
            }
        }

    /**
     * Deletes the parts the new manifest does not list. Best effort: the backup already exists, so a failure here only
     * leaves garbage for the next backup to sweep.
     */
    private suspend fun sweepStaleParts(uid: String, keep: Set<String>) {
        try {
            val stale = cloudBackupStore.listPartIds(uid).filterNot { it in keep }
            if (stale.isNotEmpty()) cloudBackupStore.deleteParts(uid, stale)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    private fun partId(snapshotId: String, stream: String, index: Int) = "$snapshotId-$stream-$index"

    private fun BackupManifestDocument.toBackupInfo() = BackupInfo(
        createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
        listCount = listCount,
        gameCount = gameCount
    )

    private class ChunkedStream(val name: String, val byteLength: Long, val parts: List<String>)
}
