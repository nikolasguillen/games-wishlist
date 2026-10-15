package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** A platform row, for the platforms the user owns. */
@Serializable
internal data class BackupPlatform(
    val id: Int,
    val name: String,
    val abbreviation: String? = null,
    val generation: Int? = null,
    val category: Int? = null,
    val platformFamily: Int? = null
)
