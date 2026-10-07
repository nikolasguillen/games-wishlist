package com.nikolasguillen.questlog.feature.onboarding.model

internal sealed interface OnboardingUiEvent {
    /**
     * What the device can do about notifications, reported once by the screen because only a composable
     * can read it. The ViewModel turns it into the page list.
     */
    data class NotificationFactsResolved(
        val requiresRuntimePermission: Boolean,
        val canDeliver: Boolean
    ) : OnboardingUiEvent

    data object FinishClicked : OnboardingUiEvent

    /** Leaves the flow from any page. Counts as completing it. */
    data object SkipClicked : OnboardingUiEvent
}
