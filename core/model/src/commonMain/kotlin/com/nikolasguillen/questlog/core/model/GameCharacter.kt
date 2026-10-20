package com.nikolasguillen.questlog.core.model

/**
 * A character of a game, as a search result.
 *
 * @property id Internal IGDB unique identifier. Navigation and lookups run on this, never on [name].
 * @property name The character's name.
 * @property imageUrl The portrait, when the catalogue has one.
 */
data class GameCharacter(
    val id: Int,
    val name: String,
    val imageUrl: String?
)
