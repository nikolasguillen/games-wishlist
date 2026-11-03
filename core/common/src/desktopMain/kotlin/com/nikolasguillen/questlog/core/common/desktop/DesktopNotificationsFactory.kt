package com.nikolasguillen.questlog.core.common.desktop

/** The one [DesktopNotifications] of the process. */
fun desktopNotifications(): DesktopNotifications = notifications

// No OS implementation is wired yet: the Windows and macOS ones replace this in specs/020-desktop-app T058 and T061.
private val notifications: DesktopNotifications by lazy { UnsupportedDesktopNotifications }
