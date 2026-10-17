package com.nikolasguillen.questlog.shared

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldValue
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass
import com.nikolasguillen.questlog.core.designsystem.theme.AppComponentsColors
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import org.jetbrains.compose.resources.stringResource

/**
 * The app's top-level navigation around [content]: a bottom bar on a portrait phone and a side rail on every other
 * window, both showing [TopLevelDestination.entries].
 *
 * The component slides away while [isVisible] is false, which is every route that is not a top-level destination.
 * The scaffold also hands [content] the insets the visible component has not used, so a screen does not pad for them
 * twice.
 * */
@Composable
internal fun QuestLogNavigationSuite(
    currentRoute: NavKey,
    isVisible: Boolean,
    onNavigateToRoute: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scaffoldState = rememberNavigationSuiteScaffoldState(
        initialValue = if (isVisible) NavigationSuiteScaffoldValue.Visible else NavigationSuiteScaffoldValue.Hidden
    )
    LaunchedEffect(isVisible) {
        if (isVisible) scaffoldState.show() else scaffoldState.hide()
    }

    val barColor = MaterialTheme.appColors.navBarContainerColor
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = AppComponentsColors.navBarItemColors,
        navigationRailItemColors = AppComponentsColors.navRailItemColors
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = destination.isSelected(currentRoute),
                    onClick = {
                        if (currentRoute != destination.route) {
                            onNavigateToRoute(destination.route)
                        }
                    },
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = stringResource(destination.label)
                        )
                    },
                    label = { Text(stringResource(destination.label)) },
                    colors = itemColors
                )
            }
        },
        modifier = modifier,
        layoutType = topLevelNavigationSuiteType(),
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = barColor,
            navigationRailContainerColor = barColor
        ),
        state = scaffoldState,
        content = content
    )
}

/**
 * The bar sits at the bottom only where the window is narrow and still tall: a phone held upright, or a split-screen
 * half of one. A window that is at least medium wide (a tablet, an unfolded foldable) or short (a phone on its side)
 * gets the rail.
 *
 * This is not [NavigationSuiteScaffoldDefaults.navigationSuiteType][androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults.navigationSuiteType]:
 * that one keeps a bar on a short window, which is exactly the phone-on-its-side case that needs the rail.
 */
@Composable
private fun topLevelNavigationSuiteType(): NavigationSuiteType {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val isWidthCompact = !windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val isHeightCompact = !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    return if (isWidthCompact && !isHeightCompact) {
        NavigationSuiteType.NavigationBar
    } else {
        NavigationSuiteType.NavigationRail
    }
}
