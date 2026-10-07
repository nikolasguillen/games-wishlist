package com.nikolasguillen.questlog.feature.onboarding.model

internal sealed interface OnboardingUiEffect {
    /** Show the system permission request — or, if it can no longer be shown, report it as denied. */
    data object RequestNotificationPermission : OnboardingUiEffect

    /** The flow was completed or skipped and has been recorded as such; the host decides where to go. */
    data object Finished : OnboardingUiEffect
}
