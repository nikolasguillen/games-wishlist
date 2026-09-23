package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp


@Composable
fun QuestLogTheme(
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
        surfaceVariant = NeutralMediumGrey,
        onSurfaceVariant = NeutralLightGrey,
        surfaceContainer = NeutralMediumGrey,
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark
    )

    val appColors = AppColors(
        appBackground = NeutralBlack,
        onAppBackground = NeutralWhite,
        searchBarScrolledContainerColor = NeutralMediumGrey,
        searchBarInputFieldColor = lerp(NeutralMediumGrey, NeutralWhite, 0.05f),
        expandedSearchBarColor = NeutralMediumGrey,
        navBarContainerColor = NeutralMediumGrey,
        navBarItemIndicatorColor = Gold,
        navBarItemSelectedIconColor = NeutralBlack,
        filterChipSelectedContainerColor = PrimaryContainerDark,
        filterChipSelectedContentColor = OnPrimaryContainerDark,
        cardContainerColor = SecondaryContainerDark,
        segmentedButtonSelectedColor = PrimaryContainerDark,
        segmentedButtonSelectedContentColor = OnPrimaryContainerDark,
        fabContainerColor = Gold,
        fabContentColor = NeutralBlack,
        hypeColor = Color(0xFFF44336),
        ratingCountColor = Color(0xFFFFB300)
    )

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalAppColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography
        ) {
            Surface(color = MaterialTheme.appColors.appBackground) {
                content()
            }
        }
    }
}

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
