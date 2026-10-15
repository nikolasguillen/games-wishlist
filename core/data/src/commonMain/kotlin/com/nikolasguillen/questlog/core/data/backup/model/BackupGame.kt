package com.nikolasguillen.questlog.core.data.backup.model

import kotlinx.serialization.Serializable

/**
 * A saved game: every `GameEntity` field except `lastViewedAt` (the Recently viewed history stays on the device),
 * plus the child collections.
 *
 * @property status the `GameStatus` name, or `null`. A name this build does not know reads back as `null`.
 */
@Serializable
internal data class BackupGame(
    val id: Int,
    val name: String,
    val description: String,
    val released: String? = null,
    val backgroundImage: String? = null,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val hypes: Int = 0,
    val metacritic: Int? = null,
    val gameTypeId: Int,
    val notes: String = "",
    val priority: Int? = null,
    val status: String? = null,
    val url: String? = null,
    val detailsFetchedAt: Long? = null,
    val platforms: List<BackupGamePlatform> = emptyList(),
    val genres: List<BackupGenre> = emptyList(),
    val companies: List<BackupCompany> = emptyList(),
    val engines: List<BackupEngine> = emptyList(),
    val artworks: List<BackupArtwork> = emptyList(),
    val relatedGames: List<BackupRelatedGame> = emptyList()
)
