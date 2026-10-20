package com.nikolasguillen.questlog.core.data.mapper

import com.nikolasguillen.questlog.core.model.CharacterProfile
import com.nikolasguillen.questlog.core.model.GameCharacter
import com.nikolasguillen.questlog.core.network.model.IgdbCharacter

private const val PORTRAIT_IMAGE_SIZE = "t_cover_big"

internal fun IgdbCharacter.toGameCharacter(): GameCharacter = GameCharacter(
    id = id,
    name = name,
    imageUrl = mugShot?.url?.toIgdbImageUrl(PORTRAIT_IMAGE_SIZE)
)

internal fun IgdbCharacter.toCharacterProfile(): CharacterProfile = CharacterProfile(
    id = id,
    name = name,
    imageUrl = mugShot?.url?.toIgdbImageUrl(PORTRAIT_IMAGE_SIZE),
    description = description?.takeIf { it.isNotBlank() },
    gameIds = games.orEmpty()
)
