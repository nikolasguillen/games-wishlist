package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Stub so the module compiles (specs/020-desktop-app, T023): sharing does nothing. Replaced in T045.
@Composable
actual fun rememberTextSharer(): TextSharer = remember { TextSharer { } }
