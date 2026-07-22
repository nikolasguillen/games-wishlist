package com.example.gameswishlist.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val appBackground: Color,
    val onAppBackground: Color,
    val searchBarScrolledContainerColor: Color,
    val searchBarInputFieldColor: Color,
    val expandedSearchBarColor: Color,
    val navBarContainerColor: Color,
    val navBarItemIndicatorColor: Color,
    val navBarItemSelectedIconColor: Color,
    val filterChipSelectedContainerColor: Color,
    val filterChipSelectedContentColor: Color,
    val cardContainerColor: Color,
    val segmentedButtonSelectedColor: Color,
    val segmentedButtonSelectedContentColor: Color,
    val fabContainerColor: Color,
    val fabContentColor: Color,
    val hypeColor: Color,
    val ratingCountColor: Color
)

internal val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        appBackground = Color.Unspecified,
        onAppBackground = Color.Unspecified,
        searchBarScrolledContainerColor = Color.Unspecified,
        searchBarInputFieldColor = Color.Unspecified,
        expandedSearchBarColor = Color.Unspecified,
        navBarContainerColor = Color.Unspecified,
        navBarItemIndicatorColor = Color.Unspecified,
        navBarItemSelectedIconColor = Color.Unspecified,
        filterChipSelectedContainerColor = Color.Unspecified,
        filterChipSelectedContentColor = Color.Unspecified,
        cardContainerColor = Color.Unspecified,
        segmentedButtonSelectedColor = Color.Unspecified,
        segmentedButtonSelectedContentColor = Color.Unspecified,
        fabContainerColor = Color.Unspecified,
        fabContentColor = Color.Unspecified,
        hypeColor = Color.Unspecified,
        ratingCountColor = Color.Unspecified
    )
}

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current