package com.nikolasguillen.questlog.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

// Stub so the module compiles (specs/020-desktop-app, T023): shows nothing and is dismissed at once. Replaced in T079.
@Composable
actual fun NotificationPermissionDeniedDialog(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { onDismiss() }
}
