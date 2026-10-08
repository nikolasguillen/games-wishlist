package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Hands plain text to the platform's share sheet.
 */
@Stable
class TextSharer(private val onShare: (text: String) -> Unit) {
    fun share(text: String) = onShare(text)
}

@Composable
expect fun rememberTextSharer(): TextSharer
