package com.nikolasguillen.questlog.core.domain.usecase.profile

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.CharacterProfile

/** Use case to load one character with its description and the ids of the games it appears in. */
class GetCharacterProfileUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(characterId: Int): AppResult<CharacterProfile> {
        return repository.getCharacterProfile(characterId)
    }
}
