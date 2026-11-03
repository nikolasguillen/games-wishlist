package com.nikolasguillen.questlog.core.common.desktop

/**
 * Opens the operating system's notification settings: the Windows Notifications page, or the macOS Notifications
 * pane. A failure to open them is ignored, since there is nothing else to offer.
 */
fun openSystemNotificationSettings() {
    val command = if (isMacOs()) {
        listOf("open", "x-apple.systempreferences:com.apple.Notifications-Settings.extension")
    } else {
        listOf("rundll32", "url.dll,FileProtocolHandler", "ms-settings:notifications")
    }
    runCatching { ProcessBuilder(command).start() }
}
