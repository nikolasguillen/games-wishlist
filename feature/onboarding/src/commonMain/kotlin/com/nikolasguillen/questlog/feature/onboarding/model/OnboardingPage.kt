package com.nikolasguillen.questlog.feature.onboarding.model

/**
 * The closed set of steps the welcome flow can show. The pages that exist for a given launch are decided
 * once by `buildOnboardingPages`, never stored.
 */
internal sealed interface OnboardingPage {
    data object Welcome : OnboardingPage
    data object Discover : OnboardingPage
    data object Lists : OnboardingPage
    data object Radar : OnboardingPage

    /** The Radar step on a platform without release reminders, which must not promise them. */
    data object RadarWithoutReminders : OnboardingPage

    /** The owned-platforms step. Optional, and never blocks the flow. */
    data object Platforms : OnboardingPage

    /** Explains release reminders, then asks for the notification permission. Only on devices that need one. */
    data object Reminders : OnboardingPage
}
