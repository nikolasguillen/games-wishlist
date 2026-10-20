package com.nikolasguillen.questlog.core.data.cloud

import com.nikolasguillen.questlog.core.cloud.CloudBackupStore
import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import kotlinx.coroutines.delay

/**
 * An in-memory [CloudBackupStore] that records the order of its calls in [calls].
 *
 * [failWriteNumber] makes that write (counting parts from 1) throw, [failDeleteParts] makes the sweep throw, and
 * [readManifestDelayMillis] keeps a manifest read busy long enough for a test to see two operations overlap.
 */
class FakeCloudBackupStore : CloudBackupStore {
    val calls = mutableListOf<String>()
    val manifests = mutableMapOf<String, BackupManifestDocument>()
    val parts = mutableMapOf<String, BackupPartDocument>()

    var failWriteNumber: Int? = null
    var failDeleteParts = false
    var readManifestFailure: Throwable? = null
    var readManifestDelayMillis = 0L

    var inFlightReads = 0
        private set
    var maxInFlightReads = 0
        private set

    private var partWrites = 0

    override suspend fun readManifest(uid: String): BackupManifestDocument? {
        calls += "readManifest"
        inFlightReads++
        maxInFlightReads = maxOf(maxInFlightReads, inFlightReads)
        try {
            if (readManifestDelayMillis > 0) delay(readManifestDelayMillis)
            readManifestFailure?.let { throw it }
            return manifests[uid]
        } finally {
            inFlightReads--
        }
    }

    override suspend fun writePart(uid: String, partId: String, part: BackupPartDocument) {
        partWrites++
        if (partWrites == failWriteNumber) throw CloudException.Connectivity("write $partWrites failed")
        calls += "writePart:$partId"
        parts[partId] = part
    }

    override suspend fun readPart(uid: String, partId: String): BackupPartDocument? {
        calls += "readPart:$partId"
        return parts[partId]
    }

    override suspend fun writeManifest(uid: String, manifest: BackupManifestDocument) {
        calls += "writeManifest"
        manifests[uid] = manifest
    }

    override suspend fun listPartIds(uid: String): List<String> {
        calls += "listPartIds"
        return parts.keys.toList()
    }

    override suspend fun deleteParts(uid: String, partIds: Collection<String>) {
        calls += "deleteParts"
        if (failDeleteParts) throw CloudException.Connectivity("sweep failed")
        partIds.forEach { parts.remove(it) }
    }

    override suspend fun deleteManifest(uid: String) {
        calls += "deleteManifest"
        manifests.remove(uid)
    }
}
