package com.nikolasguillen.questlog.feature.search.model

/**
 * Whose page a detail screen shows: a game character or a developer.
 *
 * Public because `:shared` builds it from the route's id when it creates the view model; the screen itself never
 * switches on anything else.
 */
sealed interface EntityDetailSubject {
    val id: Int

    /** The page of the character with this catalogue [id]. */
    data class Character(override val id: Int) : EntityDetailSubject

    /** The page of the developer with this catalogue [id]. */
    data class Developer(override val id: Int) : EntityDetailSubject
}
