package com.nikolasguillen.questlog.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a character of a game, as returned by IGDB's `/characters` endpoint.
 *
 * A search requests `name` and `mug_shot.url` only; a profile adds `description` and `games`.
 *
 * @property id Internal IGDB unique identifier for the character.
 * @property name The character's name.
 * @property description A free-text description, when the catalogue has one.
 * @property mugShot The character's portrait, when the catalogue has one.
 * @property games The ids of the games the character appears in (an id array, not expanded objects).
 */
@Serializable
data class IgdbCharacter(
    val id: Int,
    val name: String,
    val description: String? = null,
    @SerialName("mug_shot") val mugShot: IgdbImage? = null,
    val games: List<Int>? = null
)
