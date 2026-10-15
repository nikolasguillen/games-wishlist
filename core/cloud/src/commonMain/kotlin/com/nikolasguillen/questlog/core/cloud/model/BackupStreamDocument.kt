package com.nikolasguillen.questlog.core.cloud.model

import kotlinx.serialization.Serializable

/**
 * One named byte stream inside a backup, split over parts.
 *
 * @property name `data` for the database snapshot, or `cover-<listId>` for a list's cover photo.
 * @property byteLength the decoded length of the whole stream, checked on restore.
 * @property partIds the parts that make the stream, in order.
 */
@Serializable
data class BackupStreamDocument(
    val name: String,
    val byteLength: Long,
    val partIds: List<String>
)
