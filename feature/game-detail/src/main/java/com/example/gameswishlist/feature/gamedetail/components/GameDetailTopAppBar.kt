package com.example.gameswishlist.feature.gamedetail.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.gameswishlist.core.ui.R

/**
 * A dynamic TopAppBar that fades in its title and background based on scroll progress.
 *
 * @param title The title to display when visible.
 * @param alpha The opacity of the title and background (0.0 to 1.0).
 * @param onBackClick Callback for the navigation icon.
 * @param onAddToListClick Callback for the "Add to list" action.
 * @param modifier The modifier to be applied to the TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailTopAppBar(
    title: String,
    alpha: Float,
    onBackClick: () -> Unit,
    onAddToListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val getButtonBackgroundAlpha: (alpha: Float) -> Float =
        remember(alpha) { { alphaInput -> (1f - alphaInput) * 0.4f } }

    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = getButtonBackgroundAlpha(alpha)
                    )
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_content_description),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            IconButton(
                onClick = onAddToListClick,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = getButtonBackgroundAlpha(alpha)
                    )
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.add_to_list_content_description),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = alpha),
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = alpha),
        ),
        windowInsets = WindowInsets.statusBars
    )
}