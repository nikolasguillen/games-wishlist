package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument

/** Stand-in for the Firestore store on desktop, like [UnavailableCloudAuth]: with nobody signed in, every call fails. */
internal class UnavailableCloudBackupStore : CloudBackupStore {

    override suspend fun readManifest(uid: String): BackupManifestDocument? = unavailable()

    override suspend fun writePart(uid: String, partId: String, part: BackupPartDocument): Unit = unavailable()

    override suspend fun readPart(uid: String, partId: String): BackupPartDocument? = unavailable()

    override suspend fun writeManifest(uid: String, manifest: BackupManifestDocument): Unit = unavailable()

    override suspend fun listPartIds(uid: String): List<String> = unavailable()

    override suspend fun deleteParts(uid: String, partIds: Collection<String>): Unit = unavailable()

    override suspend fun deleteManifest(uid: String): Unit = unavailable()

    private fun unavailable(): Nothing = throw CloudException.Unauthenticated("Cloud is not available yet on desktop")
}
