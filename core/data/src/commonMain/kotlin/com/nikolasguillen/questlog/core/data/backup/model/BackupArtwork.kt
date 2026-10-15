package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/** One image of a game's gallery; [position] is the order it is shown in. */
@Serializable
internal data class BackupArtwork(val position: Int, val url: String)
