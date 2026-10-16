package com.nikolasguillen.questlog.core.data.repository

import com.nikolasguillen.questlog.core.cloud.CloudAuth
import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupStreamDocument
import com.nikolasguillen.questlog.core.common.AppVersionProvider
import com.nikolasguillen.questlog.core.data.backup.BACKUP_FORMAT_VERSION
import com.nikolasguillen.questlog.core.data.backup.BackupUnreadableException
import com.nikolasguillen.questlog.core.data.backup.MAX_BACKUP_PARTS
import com.nikolasguillen.questlog.core.data.backup.model.BackupSnapshot
import com.nikolasguillen.questlog.core.data.backup.joinParts
import com.nikolasguillen.questlog.core.data.backup.toParts
import com.nikolasguillen.questlog.core.data.local.WishlistCoverImageStorage
import com.nikolasguillen.questlog.core.data.mapper.toBackupSnapshot
import com.nikolasguillen.questlog.core.data.mapper.toUserDataRows
import com.nikolasguillen.questlog.core.database.dao.UserDataDao
import com.nikolasguillen.questlog.core.domain.repository.BackupRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.BackupInfo
import com.nikolasguillen.questlog.core.model.RepositoryError
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
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

    override suspend fun restore(): AppResult<BackupInfo> = operationLock.withLock {
        // The cover files this restore writes. Until the transaction commits they are garbage, so any exit before that
        // deletes them, a cancellation included.
        val imported = mutableListOf<String>()
        var committed = false
        try {
            val uid = currentUid() ?: return@withLock AppResult.failure(RepositoryError.NotSignedIn)

            // Files a killed earlier restore imported and never used.
            val oldCoverPaths = userDataDao.readCoverPaths()
            coverStorage.deleteAllExcept(oldCoverPaths.toSet())

            val manifest = cloudBackupStore.readManifest(uid)
                ?: return@withLock AppResult.failure(RepositoryError.BackupNotFound)
            if (manifest.formatVersion > BACKUP_FORMAT_VERSION) {
                return@withLock AppResult.failure(RepositoryError.BackupFromNewerVersion)
            }

            val streams = manifest.streams.associate { stream -> stream.name to downloadStream(uid, manifest, stream) }
            val data = streams[DATA_STREAM] ?: throw BackupUnreadableException("The backup has no data stream")
            val snapshot = try {
                json.decodeFromString(BackupSnapshot.serializer(), data.decodeToString())
            } catch (e: SerializationException) {
                throw BackupUnreadableException("The backup data cannot be decoded", e)
            } catch (e: IllegalArgumentException) {
                throw BackupUnreadableException("The backup data cannot be decoded", e)
            }

            val coverPaths = mutableMapOf<Long, String>()
            for (list in snapshot.lists.filter { it.hasCover }) {
                val bytes = streams["cover-${list.id}"] ?: throw BackupUnreadableException("Cover of list ${list.id} is missing")
                val path = coverStorage.import(bytes) ?: return@withLock AppResult.failure(RepositoryError.FileStorage)
                imported += path
                coverPaths[list.id] = path
            }

            userDataDao.replaceUserData(snapshot.toUserDataRows(coverPaths))
            committed = true

            // The replaced lists' own cover files are garbage now. Best effort: a leftover is swept by the next restore.
            withContext(NonCancellable) {
                oldCoverPaths.forEach { path ->
                    try {
                        coverStorage.delete(path)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
            }
            AppResult.success(manifest.toBackupInfo())
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        } finally {
            if (!committed) withContext(NonCancellable) { imported.forEach { coverStorage.delete(it) } }
        }
    }

    override suspend fun hasLocalUserData(): Boolean = userDataDao.hasUserData()

    // Parts first and the manifest last, the reverse of a backup: a failure part-way leaves a manifest whose parts may be
    // missing, which a restore reports as unreadable, rather than parts nobody can find. The user can simply delete again.
    override suspend fun deleteBackup(): AppResult<Unit> = operationLock.withLock {
        try {
            val uid = currentUid() ?: return@withLock AppResult.failure(RepositoryError.NotSignedIn)
            val partIds = cloudBackupStore.listPartIds(uid)
            if (partIds.isNotEmpty()) cloudBackupStore.deleteParts(uid, partIds)
            cloudBackupStore.deleteManifest(uid)
            AppResult.success(Unit)
        } catch (e: Exception) {
            AppResult.failure(e.toRepositoryError())
        }
    }

    /** Reads exactly the parts the manifest lists for [stream] and puts them back together, checking the length. */
    private suspend fun downloadStream(uid: String, manifest: BackupManifestDocument, stream: BackupStreamDocument): ByteArray {
        val texts = stream.partIds.map { partId ->
            val part = cloudBackupStore.readPart(uid, partId)
                ?: throw BackupUnreadableException("Part $partId is missing")
            if (part.snapshotId != manifest.snapshotId) {
                throw BackupUnreadableException("Part $partId belongs to another snapshot")
            }
            part.data
        }
        return texts.joinParts(stream.byteLength)
    }

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
