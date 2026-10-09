package com.nikolasguillen.questlog.feature.onboarding.mapper

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import com.nikolasguillen.questlog.core.ui.model.UiText
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingInfoPageUiModel
import com.nikolasguillen.questlog.feature.onboarding.model.OnboardingPage
import com.nikolasguillen.questlog.feature.onboarding.model.ReminderStepState
import com.nikolasguillen.questlog.feature.onboarding.resources.Res
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_discover_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_discover_headline
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_lists_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_lists_headline
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_radar_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_radar_headline
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_radar_without_reminders_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_reminders_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_reminders_declined_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_reminders_declined_headline
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_reminders_granted_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_reminders_granted_headline
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_reminders_headline
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_welcome_body
import com.nikolasguillen.questlog.feature.onboarding.resources.onboarding_welcome_headline

/**
 * The pages this launch shows, in order.
 *
 * The reminders page is last, and only when asking could change something: the platform offers release
 * reminders at all, the device has a runtime notification permission, and notifications cannot be
 * delivered yet.
 */
internal fun buildOnboardingPages(
    remindersAvailable: Boolean,
    requiresRuntimePermission: Boolean,
    canDeliver: Boolean
): List<OnboardingPage> = buildList {
    add(OnboardingPage.Welcome)
    add(OnboardingPage.Discover)
    add(OnboardingPage.Lists)
    add(if (remindersAvailable) OnboardingPage.Radar else OnboardingPage.RadarWithoutReminders)
    add(OnboardingPage.Platforms)
    if (remindersAvailable && requiresRuntimePermission && !canDeliver) add(OnboardingPage.Reminders)
}

/** The informational content of [this], or `null` for a page that is not purely informational. */
internal fun OnboardingPage.toInfoUiModel(): OnboardingInfoPageUiModel? = when (this) {
    OnboardingPage.Welcome -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_welcome_headline),
        body = UiText.StringResource(Res.string.onboarding_welcome_body),
        icon = null
    )

    OnboardingPage.Discover -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_discover_headline),
        body = UiText.StringResource(Res.string.onboarding_discover_body),
        icon = Icons.Default.Search
    )

    OnboardingPage.Lists -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_lists_headline),
        body = UiText.StringResource(Res.string.onboarding_lists_body),
        icon = Icons.Default.Bookmarks
    )

    OnboardingPage.Radar -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_radar_headline),
        body = UiText.StringResource(Res.string.onboarding_radar_body),
        icon = Icons.Default.CalendarMonth
    )

    OnboardingPage.RadarWithoutReminders -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_radar_headline),
        body = UiText.StringResource(Res.string.onboarding_radar_without_reminders_body),
        icon = Icons.Default.CalendarMonth
    )

    OnboardingPage.Platforms -> null

    // Its content depends on the step, so it is built by ReminderStepState.toInfoUiModel().
    OnboardingPage.Reminders -> null
}

/** What the reminders page shows for each state of its step. */
internal fun ReminderStepState.toInfoUiModel(): OnboardingInfoPageUiModel = when (this) {
    ReminderStepState.Undecided -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_reminders_headline),
        body = UiText.StringResource(Res.string.onboarding_reminders_body),
        icon = Icons.Default.Notifications
    )

    ReminderStepState.Granted -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_reminders_granted_headline),
        body = UiText.StringResource(Res.string.onboarding_reminders_granted_body),
        icon = Icons.Default.NotificationsActive
    )

    ReminderStepState.Declined -> OnboardingInfoPageUiModel(
        headline = UiText.StringResource(Res.string.onboarding_reminders_declined_headline),
        body = UiText.StringResource(Res.string.onboarding_reminders_declined_body),
        icon = Icons.Default.NotificationsOff
    )
}
