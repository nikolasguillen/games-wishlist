package com.example.gameswishlist.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable

object AppComponentsColors {
    val navBarItemColors: NavigationBarItemColors
        @Composable
        get() = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.appColors.navBarItemIndicatorColor,
            selectedIconColor = MaterialTheme.appColors.navBarItemSelectedIconColor
        )
}
