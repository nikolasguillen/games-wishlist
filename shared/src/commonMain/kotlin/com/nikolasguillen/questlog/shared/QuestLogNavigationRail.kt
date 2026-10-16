package com.nikolasguillen.questlog.shared

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.nikolasguillen.questlog.core.designsystem.theme.AppComponentsColors
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import com.nikolasguillen.questlog.core.navigation.SearchRoute
import org.jetbrains.compose.resources.stringResource

/**
 * The space the rail occupies on the screen's own edges: the system bars, plus the camera cutout, which Material's
 * default leaves out. In landscape the cutout sits on the start edge, right where the rail is.
 */
internal val QuestLogNavigationRailInsets: WindowInsets
    @Composable
    get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)

/**
 * The landscape counterpart of [QuestLogBottomBar]: the same destinations, in the same order, in a vertical rail.
 * It takes the current route rather than the back stack so it can be previewed.
 */
@Composable
internal fun QuestLogNavigationRail(
    currentRoute: NavKey,
    onNavigateToRoute: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.appColors.navBarContainerColor,
        windowInsets = QuestLogNavigationRailInsets
    ) {
        // The rail has no vertical arrangement, so weighted spacers center the items.
        Spacer(Modifier.weight(1f))
        TopLevelDestination.entries.forEach { destination ->
            NavigationRailItem(
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
                colors = AppComponentsColors.navRailItemColors
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@QuestLogPreviews
@Composable
private fun QuestLogNavigationRailPreview() {
    QuestLogTheme {
        QuestLogNavigationRail(
            currentRoute = SearchRoute,
            onNavigateToRoute = {}
        )
    }
}
