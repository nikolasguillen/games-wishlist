package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** A genre of a game. */
@Serializable
internal data class BackupGenre(val id: Int, val name: String)
