package com.nikolasguillen.questlog.core.domain.usecase.profile

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.AppResult
import com.nikolasguillen.questlog.core.model.DeveloperProfile

/** Use case to load one developer with its description and founding year. */
class GetDeveloperProfileUseCase(
    private val repository: GameRepository
) {
    suspend operator fun invoke(developerId: Int): AppResult<DeveloperProfile> {
        return repository.getDeveloperProfile(developerId)
    }
}
