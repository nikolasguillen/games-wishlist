package com.nikolasguillen.questlog.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable

object AppComponentsColors {
    val navBarItemColors: NavigationBarItemColors
        @Composable
        get() = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.appColors.navBarItemIndicatorColor,
            selectedIconColor = MaterialTheme.appColors.navBarItemSelectedIconColor
        )

    /** The side rail reuses the bottom bar's colors; Material gives the two a different colors type. */
    val navRailItemColors: NavigationRailItemColors
        @Composable
        get() = NavigationRailItemDefaults.colors(
            indicatorColor = MaterialTheme.appColors.navBarItemIndicatorColor,
            selectedIconColor = MaterialTheme.appColors.navBarItemSelectedIconColor
        )
}
