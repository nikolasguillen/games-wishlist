package com.nikolasguillen.questlog.core.domain.usecase.discover

import com.nikolasguillen.questlog.core.domain.repository.GameRepository
import com.nikolasguillen.questlog.core.model.TasteProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case to derive the user's [TasteProfile] from the games they have saved.
 *
 * Reads straight from local storage and re-emits whenever the library changes. A user who has saved
 * nothing gets [TasteProfile.EMPTY], which callers should read as "fall back to generic content"
 * rather than "this user likes nothing".
 */
class GetTasteProfileUseCase(
    private val repository: GameRepository
) {
    /**
     * @return A flow of the current profile, recomputed on every change to the saved games.
     */
    operator fun invoke(): Flow<TasteProfile> {
        return repository.getSavedGames().map { games ->
            if (games.isEmpty()) TasteProfile.EMPTY else games.toTasteProfile()
        }
    }
}
