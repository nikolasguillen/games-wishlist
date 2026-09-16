package com.example.gameswishlist.core.domain.usecase.translation

import com.example.gameswishlist.core.domain.translation.GameDescriptionTranslator
import javax.inject.Inject

/** Use case to check whether this device can translate game descriptions on-device right now. */
class IsDescriptionTranslationSupportedUseCase @Inject constructor(
    private val translator: GameDescriptionTranslator
) {
    suspend operator fun invoke(): Boolean {
        return translator.isSupported()
    }
}
