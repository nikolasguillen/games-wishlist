package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val searchBarScrolledContainerColor: Color,
    val searchBarInputFieldColor: Color,
    val expandedSearchBarColor: Color,
    val navBarContainerColor: Color,
    val navBarItemIndicatorColor: Color,
    val navBarItemSelectedIconColor: Color,
    val chipSelectedContainerColor: Color,
    val chipSelectedContentColor: Color,
    val chipBorderColor: Color,
    val chipSelectedBorderColor: Color,
    val segmentedButtonSelectedColor: Color,
    val segmentedButtonSelectedContentColor: Color,
    val fabContainerColor: Color,
    val fabContentColor: Color,
    val hypeColor: Color,
    val ratingCountColor: Color,
    // Use in place of colorScheme.primary only for bare text/icon content drawn directly on a
    // surface (not inside a primary/primaryContainer fill): Gold itself is ~1.7:1 on light
    // theme's surfaces, well under WCAG AA.
    val textOnSurface: Color
)

internal val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        searchBarScrolledContainerColor = Color.Unspecified,
        searchBarInputFieldColor = Color.Unspecified,
        expandedSearchBarColor = Color.Unspecified,
        navBarContainerColor = Color.Unspecified,
        navBarItemIndicatorColor = Color.Unspecified,
        navBarItemSelectedIconColor = Color.Unspecified,
        chipSelectedContainerColor = Color.Unspecified,
        chipSelectedContentColor = Color.Unspecified,
        chipBorderColor = Color.Unspecified,
        chipSelectedBorderColor = Color.Unspecified,
        segmentedButtonSelectedColor = Color.Unspecified,
        segmentedButtonSelectedContentColor = Color.Unspecified,
        fabContainerColor = Color.Unspecified,
        fabContentColor = Color.Unspecified,
        hypeColor = Color.Unspecified,
        ratingCountColor = Color.Unspecified,
        textOnSurface = Color.Unspecified
    )
}

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current