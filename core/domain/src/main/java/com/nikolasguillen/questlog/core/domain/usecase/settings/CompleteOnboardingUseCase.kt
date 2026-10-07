package com.nikolasguillen.questlog.core.domain.usecase.settings

import com.nikolasguillen.questlog.core.domain.settings.OnboardingPreferenceStore
import javax.inject.Inject

/** Records that the user finished or skipped the welcome flow. Safe to call more than once. */
class CompleteOnboardingUseCase @Inject constructor(
    private val onboardingPreferenceStore: OnboardingPreferenceStore
) {
    suspend operator fun invoke() {
        onboardingPreferenceStore.setOnboardingCompleted()
    }
}
