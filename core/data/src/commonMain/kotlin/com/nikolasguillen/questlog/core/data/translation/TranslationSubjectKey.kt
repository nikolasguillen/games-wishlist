package com.nikolasguillen.questlog.core.data.translation

import com.nikolasguillen.questlog.core.domain.translation.TranslationSubject

/**
 * The two columns a [TranslationSubject] is cached under: its kind and its id. The kinds are stored text, so
 * renaming one orphans every row of it.
 */
internal fun TranslationSubject.toCacheKey(): Pair<String, Int> = when (this) {
    is TranslationSubject.Game -> "game" to id
    is TranslationSubject.Character -> "character" to id
    is TranslationSubject.Developer -> "developer" to id
}
