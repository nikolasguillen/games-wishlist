package com.nikolasguillen.questlog.core.database.entity

import androidx.room.Entity

/**
 * An on-device translation of one description, cached per language.
 *
 * What was translated is a game, a character or a developer, told apart by [subjectType], because their ids
 * are only unique within their own kind: character 2134 and game 2134 are different texts. [subjectType] is a
 * plain string (`"game"`, `"character"`, `"developer"`) so this module does not depend on the domain type; the
 * mapping lives in `:core:data`. Nothing references the row by a foreign key: a translation is never shared and
 * costs nothing to lose. [sourceHash] is `description.hashCode()`, a staleness check rather than a security
 * boundary: IGDB edits descriptions in place, so a cheap hash is enough to tell a cached translation apart from
 * one made against text that has since changed.
 */
@Entity(
    tableName = "translated_descriptions",
    primaryKeys = ["subjectType", "subjectId", "languageTag"]
)
data class TranslatedDescriptionEntity(
    val subjectType: String,
    val subjectId: Int,
    val languageTag: String,
    val sourceHash: Int,
    val translatedText: String
)
