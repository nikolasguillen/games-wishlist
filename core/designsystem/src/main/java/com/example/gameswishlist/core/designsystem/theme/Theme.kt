package com.example.gameswishlist.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext


@Composable
fun GamesWishlistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme(
            primary = PrimaryDark,
            onPrimary = OnPrimaryDark,
            primaryContainer = PrimaryContainerDark,
            onPrimaryContainer = OnPrimaryContainerDark,
            secondary = SecondaryDark,
            onSecondary = OnSecondaryDark,
            secondaryContainer = SecondaryContainerDark,
            onSecondaryContainer = OnSecondaryContainerDark,
            tertiary = TertiaryDark,
            onTertiary = OnTertiaryDark,
            tertiaryContainer = TertiaryContainerDark,
            onTertiaryContainer = OnTertiaryContainerDark,
            error = ErrorDark,
            onError = OnErrorDark,
            errorContainer = ErrorContainerDark,
            onErrorContainer = OnErrorContainerDark,
            background = BackgroundDark,
            onBackground = OnBackgroundDark,
            surface = SurfaceDark,
            onSurface = OnSurfaceDark,
        )

        else -> lightColorScheme(
            primary = PrimaryLight,
            onPrimary = OnPrimaryLight,
            primaryContainer = PrimaryContainerLight,
            onPrimaryContainer = OnPrimaryContainerLight,
            secondary = SecondaryLight,
            onSecondary = OnSecondaryLight,
            secondaryContainer = SecondaryContainerLight,
            onSecondaryContainer = OnSecondaryContainerLight,
            tertiary = TertiaryLight,
            onTertiary = OnTertiaryLight,
            tertiaryContainer = TertiaryContainerLight,
            onTertiaryContainer = OnTertiaryContainerLight,
            error = ErrorLight,
            onError = OnErrorLight,
            errorContainer = ErrorContainerLight,
            onErrorContainer = OnErrorContainerLight,
            background = BackgroundLight,
            onBackground = OnBackgroundLight,
            surface = SurfaceLight,
            onSurface = OnSurfaceLight,
        )
    }

    val appColors = if (darkTheme) {
        AppColors(
            appBackground = colorScheme.onSurface,
            onAppBackground = colorScheme.onBackground,
            searchBarScrolledContainerColor = colorScheme.surfaceContainerLowest,
            searchBarInputFieldColor = colorScheme.surfaceBright,
            expandedSearchBarColor = colorScheme.surfaceContainer,
            navBarContainerColor = colorScheme.surfaceContainerHigh,
            navBarItemIndicatorColor = colorScheme.secondaryFixedDim,
            navBarItemSelectedIconColor = colorScheme.onSecondaryFixed
        )
    } else {
        AppColors(
            appBackground = colorScheme.surfaceContainerLow,
            onAppBackground = colorScheme.onSurface,
            searchBarScrolledContainerColor = colorScheme.surfaceContainerHighest,
            searchBarInputFieldColor = colorScheme.surfaceContainerLowest,
            expandedSearchBarColor = colorScheme.surfaceContainerLow,
            navBarContainerColor = colorScheme.surfaceContainerHigh,
            navBarItemIndicatorColor = colorScheme.secondaryFixedDim,
            navBarItemSelectedIconColor = colorScheme.onSecondaryFixed
        )
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
