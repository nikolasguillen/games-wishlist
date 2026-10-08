package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.runtime.Composable

/**
 * Never composed on iOS: it is only shown after a release-reminder opt-in, and every entry point to that is hidden
 * while `ReleaseRemindersAvailability.isAvailable` is false. It exists so `commonMain` compiles.
 */
@Composable
actual fun NotificationPermissionDeniedDialog(onDismiss: () -> Unit) = Unit
