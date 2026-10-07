package com.nikolasguillen.questlog.feature.onboarding.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.onboarding.R
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingInfoPageUiModel
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingPage

/**
 * The pages this launch shows, in order.
 *
 * Both parameters are the device facts the setup pages depend on. The informational pages do not use
 * them, so they are accepted ahead of the setup pages that will.
 */
@Suppress("UNUSED_PARAMETER")
internal fun buildOnboardingPages(
    requiresRuntimePermission: Boolean,
    canDeliver: Boolean
): List<OnboardingPage> = listOf(
    OnboardingPage.Welcome,
    OnboardingPage.Discover,
    OnboardingPage.Lists,
    OnboardingPage.Radar
)

/** The informational content of [this], or `null` for a page that is not purely informational. */
internal fun OnboardingPage.toInfoUiModel(): OnboardingInfoPageUiModel? = when (this) {
    OnboardingPage.Welcome -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(R.string.onboarding_welcome_headline),
        body = UiText.StringResource(R.string.onboarding_welcome_body),
        icon = null
    )

    OnboardingPage.Discover -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(R.string.onboarding_discover_headline),
        body = UiText.StringResource(R.string.onboarding_discover_body),
        icon = Icons.Default.Search
    )

    OnboardingPage.Lists -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(R.string.onboarding_lists_headline),
        body = UiText.StringResource(R.string.onboarding_lists_body),
        icon = Icons.Default.Bookmarks
    )

    OnboardingPage.Radar -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(R.string.onboarding_radar_headline),
        body = UiText.StringResource(R.string.onboarding_radar_body),
        icon = Icons.Default.CalendarMonth
    )
}
