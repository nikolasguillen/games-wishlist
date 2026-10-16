package com.nikolasguillen.questlog.shared

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.nikolasguillen.questlog.core.designsystem.theme.AppComponentsColors
import com.nikolasguillen.questlog.core.designsystem.theme.appColors
import org.jetbrains.compose.resources.stringResource

@Composable
fun QuestLogBottomBar(
    backStack: NavBackStack<NavKey>,
    onNavigateToRoute: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentRoute = backStack.last()

    NavigationBar(
        containerColor = MaterialTheme.appColors.navBarContainerColor,
        modifier = modifier
    ) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
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
                colors = AppComponentsColors.navBarItemColors
            )
        }
    }
}
