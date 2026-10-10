package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nikolasguillen.questlog.core.ui.util.modifiers.rainbowMetallicTint
import com.nikolasguillen.questlog.feature.gamedetail.resources.Res
import com.nikolasguillen.questlog.feature.gamedetail.resources.powered_by_apple_intelligence
import com.nikolasguillen.questlog.feature.gamedetail.resources.powered_by_apple_intelligence_description
import org.jetbrains.compose.resources.stringResource

/**
 * The badge for translations produced by Apple Intelligence, shown on iOS. Apple's own glyph is their
 * trademarked artwork, so this uses a neutral sparkle instead.
 */
@Composable
internal fun AppleIntelligenceBadge(modifier: Modifier = Modifier) {
    TranslationBadge(
        title = stringResource(Res.string.powered_by_apple_intelligence),
        message = stringResource(Res.string.powered_by_apple_intelligence_description),
        icon = {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                // Opaque, so rainbowMetallicTint keeps only the sparkle's shape.
                tint = Color.Black,
                modifier = Modifier
                    .size(TranslationBadgeIconSize)
                    .rainbowMetallicTint()
            )
        },
        modifier = modifier
    )
}
