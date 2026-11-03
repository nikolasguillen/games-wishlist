package com.nikolasguillen.questlog.core.common.desktop

import com.nikolasguillen.questlog.core.common.desktop.macos.MacUserNotifications

/** The one [DesktopNotifications] of the process, picked from the operating system. */
fun desktopNotifications(): DesktopNotifications = notifications

// Windows has no binding yet: it reports "unsupported" until the toast notifications land (specs/020-desktop-app, T058).
private val notifications: DesktopNotifications by lazy {
    if (isMacOs()) MacUserNotifications() else UnsupportedDesktopNotifications
}
