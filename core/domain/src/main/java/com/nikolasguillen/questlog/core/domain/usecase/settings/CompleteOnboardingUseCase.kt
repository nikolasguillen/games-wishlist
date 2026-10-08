package com.nikolasguillen.questlog.core.domain.usecase.settings

import com.nikolasguillen.questlog.core.domain.settings.OnboardingPreferenceStore

/** Records that the user finished or skipped the welcome flow. Safe to call more than once. */
class CompleteOnboardingUseCase(
    private val onboardingPreferenceStore: OnboardingPreferenceStore
) {
    suspend operator fun invoke() {
        onboardingPreferenceStore.setOnboardingCompleted()
    }
}
