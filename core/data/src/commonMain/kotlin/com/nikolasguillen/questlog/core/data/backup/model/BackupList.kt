package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/**
 * One list. The cover path is a location on one device, so it is not backed up: [hasCover] says whether the `cover-<id>`
 * stream holds the bytes.
 *
 * @property icon the `WishlistIcon` name, or `null`.
 */
@Serializable
internal data class BackupList(
    val id: Long,
    val name: String,
    val description: String,
    val icon: String? = null,
    val hasCover: Boolean = false
)
