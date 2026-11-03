package com.nikolasguillen.questlog.core.common.desktop

import com.nikolasguillen.questlog.core.common.desktop.macos.MacUserNotifications
import com.nikolasguillen.questlog.core.common.desktop.windows.WindowsToastNotifications

/** The one [DesktopNotifications] of the process, picked from the operating system. */
fun desktopNotifications(): DesktopNotifications = notifications

private val notifications: DesktopNotifications by lazy {
    when {
        isMacOs() -> MacUserNotifications()
        isWindows() -> WindowsToastNotifications()
        else -> UnsupportedDesktopNotifications
    }
}
