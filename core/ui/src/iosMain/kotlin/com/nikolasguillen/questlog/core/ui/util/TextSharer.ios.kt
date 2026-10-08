package com.nikolasguillen.questlog.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberTextSharer(): TextSharer = remember {
    TextSharer { text ->
        val top = topViewController() ?: return@TextSharer
        val sheet = UIActivityViewController(activityItems = listOf(text), applicationActivities = null)
        // An iPad shows the sheet as a popover and crashes without somewhere to anchor it.
        sheet.popoverPresentationController?.let { popover ->
            popover.sourceView = top.view
            val (width, height) = top.view.bounds.useContents { size.width to size.height }
            popover.sourceRect = CGRectMake(width / 2, height / 2, 0.0, 0.0)
        }
        top.presentViewController(sheet, animated = true, completion = null)
    }
}
