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

    /** The owned-platforms step. Optional, and never blocks the flow. */
    data object Platforms : OnboardingPage
}
