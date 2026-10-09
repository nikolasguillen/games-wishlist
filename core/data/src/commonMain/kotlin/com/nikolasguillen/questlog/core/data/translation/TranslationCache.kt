package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.database.dao.TranslationDao
import com.nikolasguillen.questlog.core.database.entity.TranslatedDescriptionEntity

/**
 * The engine-independent half of translating a description: the guard, the Room cache keyed by game and
 * language, the staleness check against [TranslatedDescriptionEntity.sourceHash], and sanitizing what a small
 * on-device model leaves behind. Each platform's translator supplies only [generate].
 */
internal suspend fun TranslationDao.translateWithCache(
    gameId: Int,
    languageTag: String,
    description: String,
    generate: suspend () -> String?
): String? {
    if (description.isBlank() || description.length > MAX_TRANSLATABLE_CHARS) return null

    val sourceHash = description.hashCode()
    val cached = getTranslation(gameId, languageTag)
    if (cached != null && cached.sourceHash == sourceHash) {
        // Rows written before the prompt fix can still carry a leaked label; sanitize on read too.
        return cached.translatedText.stripTranslationArtifacts()
    }

    val translated = generate()
    if (translated.isNullOrBlank()) return null

    val sanitized = translated.stripTranslationArtifacts()
    saveTranslation(
        TranslatedDescriptionEntity(
            gameId = gameId,
            languageTag = languageTag,
            sourceHash = sourceHash,
            translatedText = sanitized
        )
    )
    return sanitized
}

// A cheap first gate shared by every engine; each engine may refuse earlier against its own context window
// (iOS counts tokens). No IGDB summary comes close to this.
internal const val MAX_TRANSLATABLE_CHARS = 8000
