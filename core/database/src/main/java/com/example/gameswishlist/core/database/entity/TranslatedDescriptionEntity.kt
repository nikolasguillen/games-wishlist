package com.example.gameswishlist.core.database.entity

import androidx.room.Entity

/**
 * An on-device translation of one game's description, cached per language.
 *
 * A child table of `games`, like [GameArtworkEntity] — never a cross-ref, since a translation belongs to
 * exactly one game and is never shared. [sourceHash] is `description.hashCode()`, a staleness check
 * rather than a security boundary: IGDB edits summaries in place, so a cheap hash is enough to tell a
 * cached translation apart from one made against text that has since changed.
 */
@Entity(
    tableName = "translated_descriptions",
    primaryKeys = ["gameId", "languageTag"]
)
data class TranslatedDescriptionEntity(
    val gameId: Int,
    val languageTag: String,
    val sourceHash: Int,
    val translatedText: String
)
