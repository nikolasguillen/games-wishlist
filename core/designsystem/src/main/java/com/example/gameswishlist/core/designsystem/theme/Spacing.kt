package com.example.gameswishlist.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val default: Dp = 0.dp,
    val extraSmall: Dp = 2.dp,
    val small: Dp = 4.dp,
    val smallMedium: Dp = 6.dp,
    val medium: Dp = 8.dp,
    val mediumLarge: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val doubleLarge: Dp = 32.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
