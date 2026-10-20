package com.nikolasguillen.questlog.core.model

/**
 * One character with everything its detail page shows.
 *
 * @property id Internal IGDB unique identifier.
 * @property name The character's name.
 * @property imageUrl The portrait, when the catalogue has one.
 * @property description The catalogue's description, or `null` when it has none (a blank one counts as none).
 * @property gameIds The ids of the games the character appears in. Empty when the catalogue links none.
 */
data class CharacterProfile(
    val id: Int,
    val name: String,
    val imageUrl: String?,
    val description: String?,
    val gameIds: List<Int>
)
