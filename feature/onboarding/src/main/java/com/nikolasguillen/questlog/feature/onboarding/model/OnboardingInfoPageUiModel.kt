package com.nikolasguillen.questlog.feature.onboarding.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * One informational page. A `null` [icon] means the page shows the controller animation instead — the
 * Welcome page, which has no single icon that says "this whole app".
 */
@Immutable
internal data class OnboardingInfoPageUiModel(
    val headline: UiText,
    val body: UiText,
    val icon: ImageVector?
)
