package com.nikolasguillen.questlog.feature.search.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme

/**
 * The artwork a Discover hero shows in place of a cover that is missing or failed to load. Shared by the compact
 * and the wide hero.
 */
@Composable
internal fun HeroImageFallback() {
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

@QuestLogPreviews
@Composable
private fun HeroImageFallbackPreview() {
    QuestLogTheme {
        Box(modifier = Modifier.size(200.dp)) {
            HeroImageFallback()
        }
    }
}
