package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.runtime.Composable

/**
 * Shown right after the user opts in to a release reminder but the system notification permission is
 * denied, so the opt-in is never left silently "on" with no way to ever notify (FR-012). Its action opens the
 * platform's notification settings, so each platform supplies it.
 */
@Composable
expect fun NotificationPermissionDeniedDialog(onDismiss: () -> Unit)
