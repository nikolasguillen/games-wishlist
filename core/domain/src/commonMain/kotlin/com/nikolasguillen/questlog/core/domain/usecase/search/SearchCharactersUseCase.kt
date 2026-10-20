package com.nikolasguillen.questlog.core.domain.usecase.search

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.GameCharacter

/** Use case to search for game characters by name. The list keeps the catalogue's relevance order. */
class SearchCharactersUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(query: String): AppResult<List<GameCharacter>> {
        return repository.searchCharacters(query)
    }
}
