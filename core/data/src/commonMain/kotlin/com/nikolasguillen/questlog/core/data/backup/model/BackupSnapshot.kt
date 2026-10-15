package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/**
 * Everything a cloud backup carries, as one JSON document (the `data` stream). Cover photos travel in streams of
 * their own, named `cover-<listId>`.
 *
 * @property formatVersion the format version of the build that wrote it.
 * @property createdAtEpochMillis the writer's clock.
 * @property appVersion the writer's version name, for support only.
 * @property lists every list.
 * @property defaultListId the list the default-wishlist pointer row points at.
 * @property games every saved game with its child collections.
 * @property memberships which game sits in which list.
 * @property ownedPlatformIds the platforms the user declared they own.
 * @property ownedPlatforms the rows behind [ownedPlatformIds].
 */
@Serializable
internal data class BackupSnapshot(
    val formatVersion: Int,
    val createdAtEpochMillis: Long,
    val appVersion: String,
    val lists: List<BackupList>,
    val defaultListId: Long,
    val games: List<BackupGame>,
    val memberships: List<BackupMembership>,
    val ownedPlatformIds: List<Int> = emptyList(),
    val ownedPlatforms: List<BackupPlatform> = emptyList()
)
