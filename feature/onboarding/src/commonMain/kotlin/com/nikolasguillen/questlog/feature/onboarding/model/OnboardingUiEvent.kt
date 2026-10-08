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

    /** A tap on a platform row. Saved immediately — the step has no confirm button. */
    data class PlatformToggled(val platformId: Int) : OnboardingUiEvent

    data object ClearPlatformQuery : OnboardingUiEvent

    /** "Retry" on the platforms step's empty state, re-syncing the platform catalogue. */
    data object RetryPlatformSync : OnboardingUiEvent

    /** "Allow notifications" on the reminders page. The system request is shown only as a result of this. */
    data object AllowNotificationsClicked : OnboardingUiEvent

    data object NotNowClicked : OnboardingUiEvent

    /** The outcome of the system permission request. */
    data class NotificationPermissionResult(val granted: Boolean) : OnboardingUiEvent

    /** Notifications can now (or can no longer) be delivered, e.g. after the user changed it in system settings. */
    data class PermissionStateChanged(val canDeliver: Boolean) : OnboardingUiEvent

    data object FinishClicked : OnboardingUiEvent

    /** Leaves the flow from any page. Counts as completing it. */
    data object SkipClicked : OnboardingUiEvent
}
