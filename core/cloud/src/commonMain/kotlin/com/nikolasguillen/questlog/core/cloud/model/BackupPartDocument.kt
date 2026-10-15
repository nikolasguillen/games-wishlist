package com.nikolasguillen.questlog.core.cloud.model

import kotlinx.serialization.Serializable

/**
 * The document at `users/{uid}/parts/{partId}`, where the part id is `"$snapshotId-$stream-$index"`.
 *
 * @property snapshotId lets a sweep tell the current snapshot's parts from stale ones.
 * @property stream the name of the stream this part belongs to.
 * @property index the position of the part within its stream.
 * @property data base64 text, **at most 700,000 characters** so the document stays under Firestore's 1 MiB limit.
 *   The bound is enforced by the writer in `:core:data`, not here.
 */
@Serializable
data class BackupPartDocument(
    val snapshotId: String,
    val stream: String,
    val index: Int,
    val data: String
)
