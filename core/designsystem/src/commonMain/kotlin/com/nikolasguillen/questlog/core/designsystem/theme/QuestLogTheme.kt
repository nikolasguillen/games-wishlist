package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The resolved light/dark flag [QuestLogTheme] was actually composed with — not the raw
 * [com.nikolasguillen.questlog.core.model.AppearanceMode] preference, which alone can't say
 * which theme is showing when the user follows the system. Read via [isDarkTheme] instead of
 * re-deriving this (e.g. from a color's luminance) further down the tree.
 */
private val LocalDarkTheme = staticCompositionLocalOf { true }

@Composable
fun QuestLogTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
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
            surfaceVariant = NeutralSoftGrey,
            onSurfaceVariant = NeutralMediumGrey,
            surfaceContainer = NeutralSoftGrey,
            outline = OutlineLight,
            outlineVariant = OutlineVariantLight
        )
    }

    val appColors = if (darkTheme) {
        AppColors(
            searchBarScrolledContainerColor = NeutralMediumGrey,
            searchBarInputFieldColor = lerp(NeutralMediumGrey, NeutralWhite, 0.05f),
            expandedSearchBarColor = NeutralMediumGrey,
            navBarContainerColor = NeutralMediumGrey,
            navBarItemIndicatorColor = Gold,
            navBarItemSelectedIconColor = NeutralBlack,
            chipSelectedContainerColor = PrimaryContainerDark,
            chipSelectedContentColor = OnPrimaryContainerDark,
            chipBorderColor = NeutralWhite.copy(alpha = 0.2f),
            chipSelectedBorderColor = Color.Transparent,
            segmentedButtonSelectedColor = PrimaryContainerDark,
            segmentedButtonSelectedContentColor = OnPrimaryContainerDark,
            fabContainerColor = Gold,
            fabContentColor = NeutralBlack,
            hypeColor = HypeRed,
            ratingCountColor = RatingAmber,
            textOnSurface = Gold,
            ratingHighColor = RatingHighDark,
            ratingMidColor = RatingMidDark,
            ratingLowColor = RatingLowDark,
            createCardOutlineColor = PrimaryDark,
            createCardIconContainerColor = PrimaryDark.copy(alpha = 0.12f),
            createCardIconContentColor = Gold,
            createCardIconBorderColor = OutlineDark
        )
    } else {
        AppColors(
            searchBarScrolledContainerColor = NeutralSoftGrey,
            searchBarInputFieldColor = lerp(NeutralSoftGrey, NeutralBlack, 0.05f),
            expandedSearchBarColor = NeutralSoftGrey,
            navBarContainerColor = NeutralSoftGrey,
            navBarItemIndicatorColor = Gold,
            navBarItemSelectedIconColor = NeutralBlack,
            chipSelectedContainerColor = PrimaryContainerLight,
            chipSelectedContentColor = OnPrimaryContainerLight,
            chipBorderColor = OutlineVariantLight,
            chipSelectedBorderColor = OnPrimaryContainerLight,
            segmentedButtonSelectedColor = PrimaryContainerLight,
            segmentedButtonSelectedContentColor = OnPrimaryContainerLight,
            fabContainerColor = Gold,
            fabContentColor = NeutralBlack,
            hypeColor = HypeRed,
            ratingCountColor = RatingAmber,
            textOnSurface = GoldDeep,
            ratingHighColor = RatingHighLight,
            ratingMidColor = RatingMidLight,
            ratingLowColor = RatingLowLight,
            createCardOutlineColor = NeutralBlack,
            createCardIconContainerColor = PrimaryLight,
            createCardIconContentColor = OnPrimaryLight,
            createCardIconBorderColor = OnPrimaryLight
        )
    }

    SystemBarsAppearance(darkTheme)

    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalAppColors provides appColors,
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography
        ) {
            Surface(color = colorScheme.background) {
                content()
            }
        }
    }
}

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current

val MaterialTheme.isDarkTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalDarkTheme.current
