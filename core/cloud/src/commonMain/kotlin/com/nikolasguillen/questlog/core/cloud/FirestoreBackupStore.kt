package com.nikolasguillen.questlog.core.cloud

import com.nikolasguillen.questlog.core.cloud.error.CloudException
import com.nikolasguillen.questlog.core.cloud.error.toCloudException
import com.nikolasguillen.questlog.core.cloud.model.BackupManifestDocument
import com.nikolasguillen.questlog.core.cloud.model.BackupPartDocument
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Source
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.firestoreSettings
import dev.gitlive.firebase.firestore.memoryCacheSettings
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException

/** A Firestore batch accepts 500 operations; stay well under it. */
private const val DELETE_BATCH_SIZE = 400

internal class FirestoreBackupStore(
    private val firestore: FirebaseFirestore = Firebase.firestore
) : CloudBackupStore {

    init {
        // Memory cache only, so nothing of the backup is persisted on the device (research R11). Firestore refuses
        // new settings once it has been used; the cache is a privacy nicety, because every read asks the server, so
        // that case is not worth a crash.
        try {
            firestore.settings = firestoreSettings { cacheSettings = memoryCacheSettings { } }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
        }
    }

    override suspend fun readManifest(uid: String): BackupManifestDocument? = call {
        val snapshot = manifest(uid).get(Source.SERVER)
        if (snapshot.exists) snapshot.data(BackupManifestDocument.serializer()) else null
    }

    override suspend fun writePart(uid: String, partId: String, part: BackupPartDocument) = call {
        part(uid, partId).set(BackupPartDocument.serializer(), part)
    }

    override suspend fun readPart(uid: String, partId: String): BackupPartDocument? = call {
        val snapshot = part(uid, partId).get(Source.SERVER)
        if (snapshot.exists) snapshot.data(BackupPartDocument.serializer()) else null
    }

    override suspend fun writeManifest(uid: String, manifest: BackupManifestDocument) = call {
        manifest(uid).set(BackupManifestDocument.serializer(), manifest)
    }

    override suspend fun listPartIds(uid: String): List<String> = call {
        firestore.collection(partsPath(uid)).get(Source.SERVER).documents.map { it.id }
    }

    override suspend fun deleteParts(uid: String, partIds: Collection<String>) {
        partIds.chunked(DELETE_BATCH_SIZE).forEach { chunk ->
            call {
                val batch = firestore.batch()
                chunk.forEach { batch.delete(part(uid, it)) }
                batch.commit()
            }
        }
    }

    override suspend fun deleteManifest(uid: String) = call { manifest(uid).delete() }

    private fun manifest(uid: String): DocumentReference = firestore.document("users/$uid")

    private fun part(uid: String, partId: String): DocumentReference = firestore.document("${partsPath(uid)}/$partId")

    private fun partsPath(uid: String) = "users/$uid/parts"

    /** Bounds one Firebase call and turns its failures into this module's own exceptions. */
    private suspend fun <T> call(block: suspend () -> T): T = try {
        withTimeout(CLOUD_CALL_TIMEOUT) { block() }
    } catch (e: TimeoutCancellationException) {
        throw CloudException.Timeout("A Firestore call took longer than $CLOUD_CALL_TIMEOUT", e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        throw e.toCloudException()
    }
}
