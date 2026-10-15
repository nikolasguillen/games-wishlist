package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** A game related to another, with the name and cover it is listed under. */
@Serializable
internal data class BackupRelatedGame(
    val relatedGameId: Int,
    val name: String,
    val coverUrl: String? = null,
    val relationType: String
)
