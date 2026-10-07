package com.nikolasguillen.questlog.feature.onboarding.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.ui.model.PlatformPickerContentState

@Immutable
internal data class OnboardingUiState(
    val contentState: OnboardingContentState = OnboardingContentState.Loading,
    val platformPicker: PlatformPickerContentState = PlatformPickerContentState.Loading,
    /** The stored count, not the rendered one: a search query narrows the list but not the selection. */
    val selectedPlatformCount: Int = 0,
    val reminderStep: ReminderStepState = ReminderStepState.Undecided
)
