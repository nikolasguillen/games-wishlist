package com.nikolasguillen.questlog.feature.onboarding.model

internal sealed interface OnboardingUiEffect {
    /** The flow was completed or skipped and has been recorded as such; the host decides where to go. */
    data object Finished : OnboardingUiEffect
}
