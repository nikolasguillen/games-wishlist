package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** An engine a game is built on. */
@Serializable
internal data class BackupEngine(val id: Int, val name: String)
