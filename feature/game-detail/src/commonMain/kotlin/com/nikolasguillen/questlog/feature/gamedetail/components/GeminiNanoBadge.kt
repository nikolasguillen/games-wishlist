package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nikolasguillen.questlog.feature.gamedetail.resources.Res
import com.nikolasguillen.questlog.feature.gamedetail.resources.ic_gemini
import com.nikolasguillen.questlog.feature.gamedetail.resources.powered_by_gemini_nano
import com.nikolasguillen.questlog.feature.gamedetail.resources.powered_by_gemini_nano_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The badge for translations produced by Gemini Nano, shown on Android. */
@Composable
internal fun GeminiNanoBadge(modifier: Modifier = Modifier) {
    TranslationBadge(
        title = stringResource(Res.string.powered_by_gemini_nano),
        message = stringResource(Res.string.powered_by_gemini_nano_description),
        icon = {
            Icon(
                painter = painterResource(Res.drawable.ic_gemini),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(TranslationBadgeIconSize)
            )
        },
        modifier = modifier
    )
}
