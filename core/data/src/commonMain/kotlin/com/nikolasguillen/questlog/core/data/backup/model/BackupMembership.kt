package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** A game that sits in a list. */
@Serializable
internal data class BackupMembership(val gameId: Int, val listId: Long)
