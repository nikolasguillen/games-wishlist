package com.example.gameswishlist.core.ui.component

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.gameswishlist.core.designsystem.theme.appColors

@Composable
fun CustomFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.appColors.fabContainerColor,
        contentColor = MaterialTheme.appColors.fabContentColor,
        modifier = modifier
    ) {
        content()
    }
}