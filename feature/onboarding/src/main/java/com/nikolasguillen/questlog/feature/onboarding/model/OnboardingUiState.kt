package com.nikolasguillen.questlog.feature.onboarding.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class OnboardingUiState(
    val contentState: OnboardingContentState = OnboardingContentState.Loading
)
