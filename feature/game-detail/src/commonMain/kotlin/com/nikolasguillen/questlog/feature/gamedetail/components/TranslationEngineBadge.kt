package com.nikolasguillen.questlog.feature.gamedetail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogPreviews
import com.nikolasguillen.questlog.core.designsystem.theme.QuestLogTheme
import com.nikolasguillen.questlog.core.designsystem.theme.spacing

/**
 * Credits the on-device model that translates descriptions on this platform: [GeminiNanoBadge] on Android,
 * [AppleIntelligenceBadge] on iOS. Tapping it explains that no text left the device.
 *
 * Each `actual` only picks a variant, and both variants live in common code so the preview can show them
 * side by side: Android Studio only compiles previews against the Android `actual`.
 */
@Composable
internal expect fun TranslationEngineBadge(modifier: Modifier = Modifier)

@QuestLogPreviews
@Composable
private fun TranslationEngineBadgePreview() {
    QuestLogTheme {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)) {
            GeminiNanoBadge()
            AppleIntelligenceBadge()
        }
    }
}
