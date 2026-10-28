package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

// Desktop has no share sheet, so sharing copies the text to the clipboard; the caller confirms it to the user.
@Composable
actual fun rememberTextSharer(): TextSharer = remember {
    TextSharer(copiesToClipboard = true) { text ->
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
