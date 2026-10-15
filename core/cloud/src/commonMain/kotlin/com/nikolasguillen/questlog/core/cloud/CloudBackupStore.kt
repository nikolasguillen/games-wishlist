package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument

/**
 * The backup documents in the cloud (paths and rules are in `contracts/firestore.md`). Every member throws only
 * `Cloud*Exception` types and `CancellationException`.
 */
interface CloudBackupStore {
    /** Reads `users/{uid}` from the server, never from a cache. `null` when there is no backup. */
    suspend fun readManifest(uid: String): BackupManifestDocument?

    suspend fun writePart(uid: String, partId: String, part: BackupPartDocument)

    /** `null` when the part does not exist. */
    suspend fun readPart(uid: String, partId: String): BackupPartDocument?

    /** The commit point of a backup: once this returns, the new snapshot is the backup. */
    suspend fun writeManifest(uid: String, manifest: BackupManifestDocument)

    /** Every part document under the user, whichever snapshot it belongs to. */
    suspend fun listPartIds(uid: String): List<String>

    suspend fun deleteParts(uid: String, partIds: Collection<String>)

    suspend fun deleteManifest(uid: String)
}
