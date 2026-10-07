package com.nikolasguillen.questlog.feature.onboarding.model

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface OnboardingContentState {
    /** Until the device facts that decide which pages exist have arrived — one frame. */
    data object Loading : OnboardingContentState

    /** The page list, built once so the page count never shifts under the user. */
    data class Ready(val pages: List<OnboardingPage>) : OnboardingContentState
}
