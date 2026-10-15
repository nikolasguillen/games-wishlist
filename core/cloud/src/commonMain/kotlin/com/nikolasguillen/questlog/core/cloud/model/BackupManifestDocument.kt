package com.nikolasguillen.questlog.core.cloud.model

import kotlinx.serialization.Serializable

/**
 * The document at `users/{uid}`. Writing it is the commit point of a backup: until it is written the previous
 * manifest still points at the previous, untouched parts.
 *
 * @property snapshotId random, new for every backup.
 * @property formatVersion the format version of the app that wrote it.
 * @property createdAtEpochMillis the writer's clock, shown as "Last backup".
 * @property appVersion the writer's version name, for support only.
 * @property listCount how many lists the backup holds.
 * @property gameCount how many saved games the backup holds.
 * @property streams the streams that make the backup, `data` first.
 */
@Serializable
data class BackupManifestDocument(
    val snapshotId: String,
    val formatVersion: Int,
    val createdAtEpochMillis: Long,
    val appVersion: String,
    val listCount: Int,
    val gameCount: Int,
    val streams: List<BackupStreamDocument>
)
