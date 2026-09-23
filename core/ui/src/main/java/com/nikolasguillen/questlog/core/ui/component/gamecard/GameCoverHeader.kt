package com.nikolasguillen.questlog.core.ui.component.gamecard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.nikolasguillen.questlog.core.designsystem.theme.spacing
import com.nikolasguillen.questlog.core.ui.R

@Composable
internal fun GameCoverHeader(
    coverImage: String?,
    height: Dp
) {
    Box(
        modifier = Modifier
            .height(height)
            .fillMaxWidth()
    ) {
        if (coverImage != null) {
            SubcomposeAsyncImage(
                model = coverImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.extraLarge),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = R.drawable.placeholder,
                            contentDescription = null,
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                error = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ImageNotSupported,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}
