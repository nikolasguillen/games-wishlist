package com.example.gameswishlist.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun GamesWishlistTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
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
        surfaceVariant = SpotifyMediumGrey,
        onSurfaceVariant = SpotifyLightGrey
    )

    val appColors = AppColors(
        appBackground = SpotifyBlack,
        onAppBackground = SpotifyWhite,
        searchBarScrolledContainerColor = SpotifyDarkGrey,
        searchBarInputFieldColor = SpotifyMediumGrey,
        expandedSearchBarColor = SpotifyBlack,
        navBarContainerColor = SpotifyBlack.copy(alpha = 0.95f),
        navBarItemIndicatorColor = SpotifyGreen,
        navBarItemSelectedIconColor = SpotifyBlack
    )

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
