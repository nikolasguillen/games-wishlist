package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** A platform a game runs on, with the release date the game has on it. */
@Serializable
internal data class BackupGamePlatform(
    val id: Int,
    val name: String,
    val abbreviation: String? = null,
    val generation: Int? = null,
    val category: Int? = null,
    val platformFamily: Int? = null,
    val releaseDate: Long? = null,
    val releaseDatePrecision: Int? = null
)
