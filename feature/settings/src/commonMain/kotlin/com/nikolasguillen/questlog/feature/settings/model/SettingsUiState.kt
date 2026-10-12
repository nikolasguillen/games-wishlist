package com.nikolasguillen.questlog.feature.settings.model

import androidx.compose.runtime.Immutable
import com.nikolasguillen.questlog.core.model.AppearanceMode
import com.nikolasguillen.questlog.core.ui.model.UiText

/**
 * State of the Settings hub.
 *
 * There is no `ContentState` here on purpose: the groups and rows are static, and the only value that
 * has to be read is [ownedPlatformsSummary]. A four-state lifecycle around a single subtitle would add
 * a `when` that always lands on the same branch.
 *
 * @property ownedPlatformsSummary The platforms the filter currently runs on, `null` while unknown so
 * the row shows a title with no subtitle instead of flashing a wrong one.
 * @property appVersion The installed version name, read once at construction.
 * @property translationModel The on-device translation model row's state. Starts
 * [TranslationModelRowState.Hidden] and flips once the async status check resolves — the row is absent
 * for that first beat rather than shown and then withdrawn on an unsupported device.
 * @property releaseNotificationCount How many saved games currently have a release reminder enabled,
 * shown as the management row's subtitle.
 * @property notificationPermission The system notification permission's state, as reflected in the
 * "Notifications" group. Starts [NotificationPermissionRowState.Granted] so the row is absent until the
 * composition-only permission read reports otherwise, rather than flashing "blocked" on every launch.
 * @property appearanceMode The user's current Appearance selection, reflected by the segmented control in
 * the App group. Starts `SYSTEM`, matching the persisted default.
 */
@Immutable
internal data class SettingsUiState(
    val ownedPlatformsSummary: UiText? = null,
    val appVersion: String = "",
    val translationModel: TranslationModelRowState = TranslationModelRowState.Hidden,
    val releaseNotificationCount: Int = 0,
    val notificationPermission: NotificationPermissionRowState = NotificationPermissionRowState.Granted,
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM
)
