package com.example.gameswishlist.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.lerp


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
        onSurfaceVariant = SpotifyLightGrey,
        surfaceContainer = SpotifyMediumGrey
    )

    val appColors = AppColors(
        appBackground = SpotifyBlack,
        onAppBackground = SpotifyWhite,
        searchBarScrolledContainerColor = SpotifyMediumGrey,
        searchBarInputFieldColor = lerp(SpotifyMediumGrey, SpotifyWhite, 0.05f),
        expandedSearchBarColor = SpotifyMediumGrey,
        navBarContainerColor = SpotifyMediumGrey,
        navBarItemIndicatorColor = SpotifyGreen,
        navBarItemSelectedIconColor = SpotifyBlack,
        filterChipSelectedContainerColor = PrimaryContainerDark,
        filterChipSelectedContentColor = OnPrimaryContainerDark,
        cardContainerColor = SecondaryContainerDark,
        segmentedButtonSelectedColor = PrimaryContainerDark,
        segmentedButtonSelectedContentColor = OnPrimaryContainerDark,
        fabContainerColor = SpotifyGreen,
        fabContentColor = SpotifyBlack
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
