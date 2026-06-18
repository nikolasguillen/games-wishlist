package com.example.gameswishlist.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val navBarItemIndicatorColor: Color,
    val navBarItemSelectedIconColor: Color
)

internal val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        navBarItemIndicatorColor = Color.Unspecified,
        navBarItemSelectedIconColor = Color.Unspecified
    )
}

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current