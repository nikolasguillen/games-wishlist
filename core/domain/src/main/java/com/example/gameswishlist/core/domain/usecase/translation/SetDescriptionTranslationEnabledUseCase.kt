package com.example.gameswishlist.core.domain.usecase.translation

import com.example.gameswishlist.core.domain.repository.GameRepository
import javax.inject.Inject

/** Use case to switch on-device description translation on or off. */
class SetDescriptionTranslationEnabledUseCase @Inject constructor(
    private val repository: GameRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        repository.setDescriptionTranslationEnabled(enabled)
    }
}
