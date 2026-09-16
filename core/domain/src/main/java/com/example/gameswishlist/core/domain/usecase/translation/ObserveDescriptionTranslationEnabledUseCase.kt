package com.example.gameswishlist.core.domain.usecase.translation

import com.example.gameswishlist.core.domain.repository.GameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Use case to observe whether on-device description translation is switched on. Defaults to `false`. */
class ObserveDescriptionTranslationEnabledUseCase @Inject constructor(
    private val repository: GameRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return repository.isDescriptionTranslationEnabled()
    }
}
