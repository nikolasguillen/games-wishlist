package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** A company that developed or published a game. */
@Serializable
internal data class BackupCompany(
    val id: Int,
    val name: String,
    val isDeveloper: Boolean = false,
    val isPublisher: Boolean = false
)
