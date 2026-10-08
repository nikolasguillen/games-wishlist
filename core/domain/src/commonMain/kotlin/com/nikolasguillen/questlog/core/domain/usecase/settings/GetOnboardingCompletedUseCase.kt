package com.nikolasguillen.questlog.core.domain.usecase.settings

import com.nikolasguillen.questlog.core.domain.settings.OnboardingPreferenceStore
import kotlinx.coroutines.flow.Flow

/** Whether the welcome flow has been completed or skipped, and every subsequent change. */
class GetOnboardingCompletedUseCase(
    private val onboardingPreferenceStore: OnboardingPreferenceStore
) {
    operator fun invoke(): Flow<Boolean> = onboardingPreferenceStore.observeOnboardingCompleted()
}
