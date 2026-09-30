package com.nikolasguillen.questlog.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun QuestLogTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
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
            segmentedButtonSelectedColor = PrimaryContainerDark,
            segmentedButtonSelectedContentColor = OnPrimaryContainerDark,
            fabContainerColor = Gold,
            fabContentColor = NeutralBlack,
            hypeColor = HypeRed,
            ratingCountColor = RatingAmber
        )
    } else {
        AppColors(
            appBackground = NeutralOffWhite,
            onAppBackground = NeutralBlack,
            searchBarScrolledContainerColor = NeutralSoftGrey,
            searchBarInputFieldColor = lerp(NeutralSoftGrey, NeutralBlack, 0.05f),
            expandedSearchBarColor = NeutralSoftGrey,
            navBarContainerColor = NeutralSoftGrey,
            navBarItemIndicatorColor = Gold,
            navBarItemSelectedIconColor = NeutralBlack,
            filterChipSelectedContainerColor = PrimaryContainerLight,
            filterChipSelectedContentColor = OnPrimaryContainerLight,
            segmentedButtonSelectedColor = PrimaryContainerLight,
            segmentedButtonSelectedContentColor = OnPrimaryContainerLight,
            fabContainerColor = Gold,
            fabContentColor = NeutralBlack,
            hypeColor = HypeRed,
            ratingCountColor = RatingAmber
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(activity.window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

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
