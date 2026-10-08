package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.runtime.Composable

/**
 * Makes the system bars' icons readable on the theme that is showing: dark icons on light, light on dark.
 * What a "system bar" is differs per platform, so each one supplies its own.
 */
@Composable
internal expect fun SystemBarsAppearance(darkTheme: Boolean)
