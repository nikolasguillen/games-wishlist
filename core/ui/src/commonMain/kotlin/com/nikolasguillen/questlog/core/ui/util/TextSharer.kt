package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Hands plain text to the platform's share sheet.
 *
 * @property copiesToClipboard True where there is no share sheet and [share] puts the text on the clipboard instead
 * (desktop), so the caller has to tell the user that something happened.
 */
@Stable
class TextSharer(val copiesToClipboard: Boolean = false, private val onShare: (text: String) -> Unit) {
    fun share(text: String) = onShare(text)
}

@Composable
expect fun rememberTextSharer(): TextSharer
