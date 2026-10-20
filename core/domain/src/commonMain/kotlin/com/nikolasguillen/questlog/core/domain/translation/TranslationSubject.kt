package com.nikolasguillen.questlog.core.domain.translation

/**
 * What a description belongs to, and so what its cached translation is keyed by. Ids are only unique within
 * their own kind -- a game and a character can share one -- so the kind is part of the key.
 */
sealed interface TranslationSubject {
    /** A game's description. */
    data class Game(val id: Int) : TranslationSubject

    /** A game character's description. */
    data class Character(val id: Int) : TranslationSubject

    /** A developer's description. */
    data class Developer(val id: Int) : TranslationSubject
}
