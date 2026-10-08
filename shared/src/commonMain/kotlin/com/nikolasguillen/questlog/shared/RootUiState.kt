package com.nikolasguillen.questlog.shared

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.AppearanceMode

/**
 * What the app root needs before it can draw: which theme, and whether the welcome flow comes first.
 *
 * @property onboardingCompleted `null` until the stored flag has been read. The back stack's first entry is
 * chosen from it, and the back stack only looks at its initial key once, so nothing may compose before it is
 * known - and `false` must never stand in for "not read yet".
 */
@Immutable
data class RootUiState(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val onboardingCompleted: Boolean? = null
)
